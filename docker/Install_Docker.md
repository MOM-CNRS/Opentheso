# Mini documentation : Comment utiliser et lancer le Docker

## 1. Lancer l'environnement complet

Depuis le dossier `docker/`. Le build Maven se fait dans un container de build
(première étape du Dockerfile), aucun JDK ni Maven n'est requis sur la machine.

``` bash
docker compose -f Docker-compose.yml up -d
```

## 2. Vérifier que les services tournent

``` bash
docker ps
```

Vous devriez voir : - un container `opentheso-db` - un container
`opentheso`

## 3. Logs de l'application

``` bash
docker logs -f opentheso
```

## 4. Accéder à l'application

➡️ http://localhost:8099/

## 5. Arrêter les services

``` bash
docker compose -f Docker-compose.yml down      # garde la base
docker compose -f Docker-compose.yml down -v   # supprime aussi la base
```

## 6. Forcer la reconstruction

``` bash
docker compose -f Docker-compose.yml build --no-cache
docker compose -f Docker-compose.yml up -d
```

## /////// English version ////////
# Mini Documentation: How to Use and Run Docker

## 1. Start the Full Environment

From the `docker/` directory. The Maven build runs in a build container (first
stage of the Dockerfile), so no local JDK or Maven is needed.

``` bash
docker compose -f Docker-compose.yml up -d
```

## 2. Check That the Services Are Running

``` bash
docker ps
```

You should see: - a container `opentheso-db` - a container `opentheso`

## 3. View Application Logs

``` bash
docker logs -f opentheso
```

## 4. Access the Application

➡️ http://localhost:8099/

## 5. Stop the Services

``` bash
docker compose -f Docker-compose.yml down      # keeps the database
docker compose -f Docker-compose.yml down -v   # also drops the database
```

## 6. Force a Rebuild

``` bash
docker compose -f Docker-compose.yml build --no-cache
docker compose -f Docker-compose.yml up -d
```
