/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnrs.opentheso.ws.handle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author miled.rousset
 */
@Slf4j
@Service
public class HandleClient {
    @Value("${certificats.cacerts2}")
    private String cacerts2Path;

    @Value("${certificats.key}")
    private String keyPath;


    // Méthode pour créer et configurer SSLContext avec les certificats client et serveur
    private SSLContext createSSLContext(String pass) throws Exception {
        log.debug("PAsse par la création de SSLContext");
        KeyStore clientStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            clientStore.load(fis, pass.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(clientStore, pass.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(cacerts2Path)) {
            trustStore.load(fis, pass.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    public String putHandle2(String pass, String pathKey, String pathCert,
                             String urlHandle, String idHandle, String jsonData) {

        String output;
        String xmlRecord = "";

        try {
            // Création du SSLContext avec les certificats client et serveur
            SSLContext sslContext = createSSLContext(pass);  // Appel de la méthode createSSLContext

            // URL de destination
            URL url = new URL(urlHandle + idHandle);

            // Connexion HTTPS
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());  // Utilisation du SSLContext
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Handle clientCert=\"true\"");

            // Définir le HostnameVerifier pour la vérification du certificat du serveur
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // Vous pouvez ajouter une logique de vérification de l'hôte ici si nécessaire
                    return true;  // A remplacer par une vérification stricte en production
                }
            });

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Envoi des données JSON
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter out = new OutputStreamWriter(os);
            out.write(jsonData);
            out.flush();

            // Lire la réponse du serveur
            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }

            byte[] bytes = xmlRecord.getBytes();
            xmlRecord = new String(bytes, Charset.forName("UTF-8"));

            br.close();
            os.close();
            conn.disconnect();

            // Gestion de la réponse
            if (status == 200 || status == 201) {
                return getIdHandle(xmlRecord);
            }
            if (status == 100) {
                return null;
            }

        } catch (Exception ex) {
            // Gérer toutes les exceptions ici
            Log.error("Erreur Handle : ", ex.getMessage());
        }
        return null;
    }


    private String message = "";
    /**
     * Permet de récupérer l'identifiant Handle d'une resource sous forme de données en Json
     * @param urlHandle
     * @param idHandle
     * @return 
     */
    public String getHandle(
            String urlHandle,
            String idHandle) {
        
        String output;
        String xmlRecord = "";
        try {
            urlHandle = urlHandle.replace("https://", "http://");
            URL url = new URL(urlHandle + idHandle);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }
            byte[] bytes = xmlRecord.getBytes();
            xmlRecord = new String(bytes, Charset.forName("UTF-8"));
            
            if(status == 200) {
                message = "Récupération du Handle réussie";
            }
            if(status == 100) {
                message = "Handle n'existe pas";
            }
            message = message + "\n" + xmlRecord;
            message = message + "\n" + "status de la réponse : " + status;
            conn.disconnect();
            br.close();
            if(status == 200) return getIdHandle(xmlRecord);
            else
                return null;

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    
    /**
     * Permet de mettre à jour l'URL et les données d'une resource Handle
     * cette fonction donne la même action que le putHandle
     * @param pass
     * @param pathKey
     * @param pathCert
     * @param urlHandle
     * @param idHandle
     * @param jsonData
     * @return 
     */
    public boolean updateHandle(String pass,
            String pathKey, String pathCert, 
            String urlHandle, String idHandle,
            String jsonData) {

        String output;
        String xmlRecord = "";

        try {
            KeyStore clientStore = KeyStore.getInstance("PKCS12");
            //"motdepasse" = le mot de passe saisie pour la génération des certificats.
        //    clientStore.load(new FileInputStream("key.p12"), "motdepasse".toCharArray());
            clientStore.load(this.getClass().getResourceAsStream(pathKey), pass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientStore, pass.toCharArray());
            
            KeyStore trustStore = KeyStore.getInstance("JKS");
//            trustStore.load(new FileInputStream("cacerts2"), pass.toCharArray());
            trustStore.load(this.getClass().getResourceAsStream(pathCert), pass.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            //URL url = new URL("https://cchum-isi-handle01.in2p3.fr:8001/api/handles/20.500.11942/opentheso443");
            URL url = new URL(urlHandle + idHandle);
            
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Handle clientCert=\"true\"");
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();

            OutputStreamWriter out = new OutputStreamWriter(os);
            out.write(jsonData);
            out.flush();

            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            // status = 201 = création réussie

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }
            byte[] bytes = xmlRecord.getBytes();
            xmlRecord = new String(bytes, Charset.forName("UTF-8"));
            os.close();
            conn.disconnect();
            br.close();
            
            if(status == 200) {
                message = "Mise à jour du Handle réussie";
            }
            if(status == 100) {
                message = "Handle n'existe pas";
            }
            message = message + "\n" + xmlRecord;
            message = message + "\n" + "status de la réponse : " + status;
            return true;            

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CertificateException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return false;
    }
    
    /**
     * Permet de créer un identifiant Handle
     * @param pass
     * @param pathKey
     * @param pathCert
     * @param urlHandle
     * @param idHandle
     * @param jsonData
     * @return l'id du Handle
     */
    public String putHandle(String pass,
                            String pathKey, String pathCert,
                            String urlHandle, String idHandle,
                            String jsonData) {

        String output;
        String xmlRecord = "";

        try {
            // Désactiver les tickets de session
            System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");

            log.debug("avant la clé PKCS12");
            KeyStore clientStore = KeyStore.getInstance("PKCS12");

            log.debug("avant la clé KeyPath");
            try (FileInputStream fis = new FileInputStream(pathKey)) {
                clientStore.load(fis, pass.toCharArray());
                log.debug("après la lecture de KeyPath");
            } catch (MalformedURLException ex) {
                log.error("Catch de KeyPath", ex);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientStore, pass.toCharArray());

            log.debug("avant la lecture de JKS");
            KeyStore trustStore = KeyStore.getInstance("JKS");

            try (FileInputStream fis = new FileInputStream(pathCert)) {
                log.debug("avant la lecture de pathCert");
                trustStore.load(fis, pass.toCharArray());
                log.debug("après la lecture de pathCert");
            } catch (MalformedURLException ex) {
                log.error("Catch de pathCert", ex);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            log.debug("avant init TLS");
            // Utilisation explicite de TLS 1.3
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            log.debug("après init TLSv1.3");

            URL url = new URL(urlHandle + idHandle);
            log.debug("après new URL");

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            log.debug("après new URL openConnection");

            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            log.debug("après sslContext.getSocketFactory()");

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Handle clientCert=\"true\"");

            conn.setHostnameVerifier((hostname, session) -> {
                log.debug("HostnameVerifier appelé pour le hostname: " + hostname);
                return true;
            });
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            log.debug("avant la connexion de Handle");
            try (OutputStream os = conn.getOutputStream();
                 OutputStreamWriter out = new OutputStreamWriter(os)) {
                out.write(jsonData);
                out.flush();
                log.debug("au niveau du flush des json de Handle");
            }

            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            log.debug("valeur du status de Handle : " + status);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                while ((output = br.readLine()) != null) {
                    xmlRecord += output;
                }
            }

            xmlRecord = new String(xmlRecord.getBytes(), Charset.forName("UTF-8"));
            log.debug("xmlRecord de Handle : " + xmlRecord);

            conn.disconnect();
            if (status == 200 || status == 201) {
                log.debug("Création du Handle réussie");
                return getIdHandle(xmlRecord);
            } else if (status == 100) {
                log.debug("Handle n'existe pas");
                return null;
            }

        } catch (Exception ex) {
            log.error("Erreur Handle : ", ex);
            message = describeHandleException(ex);
        }
        return null;
    }

    private String describeHandleException(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : root.getClass().getSimpleName();
        if (ex instanceof java.io.FileNotFoundException
                || root instanceof java.io.FileNotFoundException) {
            return "fichier clé/certificat Handle introuvable ou chemin vide (" + detail + ")";
        }
        if (detail.contains("PKIX") || detail.contains("SSLHandshake")
                || root instanceof javax.net.ssl.SSLHandshakeException) {
            return "certificat SSL du serveur Handle non reconnu (PKIX) : " + detail;
        }
        return detail;
    }

    public String makeHttpsRequest(String method, String urlHandle, String idHandle,
                                   String jsonData, SSLContext sslContext) {
        String output = "";
        String xmlRecord = "";
        log.debug("Passe par makeHttpsRequest");
        try {
            // Création de l'URL et de la connexion HTTPS
            URL url = new URL(urlHandle + idHandle);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Configurer la connexion SSL
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Handle clientCert=\"true\"");

            // Pour des raisons de test, désactivation de la vérification du nom d'hôte
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // Accepte tous les hôtes
                }
            });

            // Configuration des flux pour envoyer et recevoir les données
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Écriture des données JSON dans le flux de sortie
            try (OutputStream os = conn.getOutputStream();
                 OutputStreamWriter out = new OutputStreamWriter(os, Charset.forName("UTF-8"))) {
                out.write(jsonData);
                out.flush();
            }

            // Lecture de la réponse de la requête
            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));

            // Construire la réponse XML
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }
            byte[] bytes = xmlRecord.getBytes(Charset.forName("UTF-8"));
            xmlRecord = new String(bytes, Charset.forName("UTF-8"));

            br.close();
            conn.disconnect();

            return xmlRecord; // Renvoie la réponse reçue
        } catch (Exception ex) {
            // Gestion des erreurs
            Log.error("Erreur lors de la requête HTTPS : ", ex.getMessage());
            return null;
        }
    }


    
    
    /**
     * Permet de supprimer l'identifiant Handle d'une resource
     * @param pass
     * @param pathKey
     * @param pathCert
     * @param urlHandle
     * @param idHandle
     * @return 
     */
    public boolean deleteHandle(String pass,
            String pathKey, String pathCert, 
            String urlHandle,
            String idHandle) {
        
        //exp : idHandle = (20.500.11942/LDx76olvIm)
        String output;
        String xmlRecord = "";
        try {
            KeyStore clientStore = KeyStore.getInstance("PKCS12");
            //"motdepasse" = le mot de passe saisie pour la génération des certificats.
        //    clientStore.load(new FileInputStream("key.p12"), "motdepasse".toCharArray());
            clientStore.load(this.getClass().getResourceAsStream(pathKey), pass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientStore, pass.toCharArray());
            
            KeyStore trustStore = KeyStore.getInstance("JKS");
//            trustStore.load(new FileInputStream("cacerts2"), pass.toCharArray());
            trustStore.load(this.getClass().getResourceAsStream(pathCert), pass.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            //URL url = new URL("https://cchum-isi-handle01.in2p3.fr:8001/api/handles/20.500.11942/opentheso443");
            URL url = new URL(urlHandle + idHandle);
            
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Handle clientCert=\"true\"");
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((output = br.readLine()) != null) {
                xmlRecord += output;
            }
            byte[] bytes = xmlRecord.getBytes();
            xmlRecord = new String(bytes, Charset.forName("UTF-8"));
            
            if(status == 200) {
                message = "Suppression du Handle réussie";
            }
            if(status == 100) {
                message = "Handle n'existe pas";
            }
            message = message + "\n" + xmlRecord;
            message = message + "\n" + "status de la réponse : " + status;
            br.close();
            return true;

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (KeyStoreException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (CertificateException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (KeyManagementException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (MalformedURLException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (IOException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } catch (Exception ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
            message = message + ex.getMessage();
        } 
        return false;
    }    

    /**
     * Permet de savoir si l'identifiant handle existe ou non sur handle.net
     * 
     * 
     * @param urlHandle
     * @param idHandle
     * @return 
     */
    public boolean isHandleExist(
            String urlHandle, String idHandle) {
        try {
            urlHandle = urlHandle.replace("http://","https://");
            URL url = new URL(urlHandle + idHandle);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            int status = conn.getResponseCode();
            conn.disconnect();
            if(status == 200) return true;
            else return false;

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HandleClient.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return false;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * permet d'initialiser un objet String de type Json 
     * en paramètre l'URL du Site, 
     * le retour sera adapté pour la création de l'identifiant Handle
     * @param urlResource
     * @return 
     */
    public String getJsonData(String urlResource) {
        // le retour des données doit être sous ce format :
        /*"{\"index\":1,\"type\":\"URL\",\"data\":{\"format\":\"string\",\"value\":\"http://toto.mom.fr\"},\"ttl\":86400,\"permissions\":\"1110\"}";
        "values":[
        {"index":1,"type":"URL","data":{"format":"string","value":"http://toto.mom.fr"},"ttl":86400,"timestamp":"2017-12-11T15:15:43Z"}
        ]
        
        {
            "index": 1,
            "type": "URL",
            "data": {
                "format": "string",
                "value": "http://www.huma-num.fr"
            },
            "ttl": 86400,
            "permissions": "1110"
        }*/       
        
        JsonObjectBuilder builder = Json.createObjectBuilder();
    
        builder.add("index", "1");
        builder.add("type", "URL");

        // pour le l'Objet dans l'Objet 
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("format", "string");
        job.add("value", urlResource);
        
        builder.add("data", job.build());
        
        builder.add("ttl", "86400");
        builder.add("permissions", "1110");
        return builder.build().toString();
    }
    
    private String getIdHandle(String jsonText) {
        if(jsonText == null) return null;
        //{"responseCode":1,"handle":"20.500.11942/opentheso443"}
        JsonReader reader = Json.createReader(new StringReader(jsonText));
        JsonObject jsonObject = reader.readObject();
        reader.close();
        JsonString values = jsonObject.getJsonString("handle");
        if(values != null)
            return values.getString();
        return null;
    }
    
    
}
