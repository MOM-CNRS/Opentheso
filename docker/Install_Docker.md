# Mini documentation : Comment utiliser et lancer le Docker

## 1. Configuration

Depuis le dossier `docker/` :

``` bash
cp .env.example .env
```

`.env` contient les identifiants base, les clés de chiffrement et le port hôte.
Les valeurs par défaut conviennent pour une évaluation locale. Compose lit
`.env` automatiquement ; le fichier est ignoré par git.

## 2. Lancer l'environnement complet

Le build Maven se fait dans un container de build (première étape du
Dockerfile), aucun JDK ni Maven n'est requis sur la machine.

``` bash
docker compose -f Docker-compose.yml up -d
```

## 3. Vérifier que les services tournent

``` bash
docker ps
```

Vous devriez voir : - un container `opentheso-db` - un container
`opentheso`

## 4. Logs de l'application

``` bash
docker logs -f opentheso
```

## 5. Accéder à l'application

➡️ http://localhost:8099/ (ou le `APP_PORT` défini dans `.env`)

## 6. Arrêter les services

``` bash
docker compose -f Docker-compose.yml down      # garde la base
docker compose -f Docker-compose.yml down -v   # supprime aussi la base
```

## 7. Forcer la reconstruction

``` bash
docker compose -f Docker-compose.yml build --no-cache
docker compose -f Docker-compose.yml up -d
```

## 8. Vers Kubernetes

Le même `.env` sert de contrat de configuration :

``` bash
kubectl create secret generic opentheso --from-env-file=.env
```

puis `envFrom: [{ secretRef: { name: opentheso } }]` dans le Deployment.
Seul `DB_HOST` change (nom du Service Postgres au lieu de `opentheso-db`).

## /////// English version ////////
# Mini Documentation: How to Use and Run Docker

## 1. Configuration

From the `docker/` directory:

``` bash
cp .env.example .env
```

`.env` holds the database credentials, the encryption keys and the host port.
The defaults are fine for local evaluation. Compose reads `.env` automatically;
the file is gitignored.

## 2. Start the Full Environment

The Maven build runs in a build container (first stage of the Dockerfile), so
no local JDK or Maven is needed.

``` bash
docker compose -f Docker-compose.yml up -d
```

## 3. Check That the Services Are Running

``` bash
docker ps
```

You should see: - a container `opentheso-db` - a container `opentheso`

## 4. View Application Logs

``` bash
docker logs -f opentheso
```

## 5. Access the Application

➡️ http://localhost:8099/ (or the `APP_PORT` set in `.env`)

## 6. Stop the Services

``` bash
docker compose -f Docker-compose.yml down      # keeps the database
docker compose -f Docker-compose.yml down -v   # also drops the database
```

## 7. Force a Rebuild

``` bash
docker compose -f Docker-compose.yml build --no-cache
docker compose -f Docker-compose.yml up -d
```

## 8. Towards Kubernetes

The same `.env` doubles as the configuration contract:

``` bash
kubectl create secret generic opentheso --from-env-file=.env
```

then `envFrom: [{ secretRef: { name: opentheso } }]` in the Deployment.
Only `DB_HOST` changes (the Postgres Service name instead of `opentheso-db`).
