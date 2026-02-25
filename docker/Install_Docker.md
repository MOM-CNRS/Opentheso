# Mini documentation : Comment utiliser et lancer le Docker

## 1. Construire l'image

À la racine du projet (là où se trouve le Dockerfile) :

``` bash
docker build -t opentheso:local .
```

## 2. Lancer l'environnement complet

``` bash
docker compose up -d
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

Si vous publiez le port 8099 :

➡️ http://localhost:8099/

## 6. Arrêter les services

``` bash
docker compose down
```

## 7. Forcer la reconstruction

``` bash
docker compose build --no-cache
docker compose up -d
```

## /////// English version ////////
# Mini Documentation: How to Use and Run Docker

## 1. Build the Image

At the root of the project (where the Dockerfile is located):

``` bash
docker build -t opentheso:local .
```

## 2. Start the Full Environment

``` bash
docker compose up -d
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

If port 8099 is published:

➡️ http://localhost:8099/

## 6. Stop the Services

``` bash
docker compose down
```

## 7. Force a Rebuild

``` bash
docker compose build --no-cache
docker compose up -d
```
