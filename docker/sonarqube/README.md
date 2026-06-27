# SonarQube local (OpenTheso)

## Démarrer SonarQube

```bash
cd docker/sonarqube
docker compose up -d
```

Premier démarrage : attendre 1 à 2 minutes, puis ouvrir [http://localhost:9000](http://localhost:9000).

Identifiants par défaut : `admin` / `admin` (changement de mot de passe demandé à la première connexion).

## Créer le projet dans SonarQube

1. **Projects** → **Create project locally**
2. **Project key** : `opentheso` (doit correspondre à `sonar-project.properties`)
3. Générer un **token** (My Account → Security → Generate Token)

## Analyser le code (Maven)

Analyse **limitée au package `fr.cnrs.opentheso.v2`** (legacy et `target/` exclus via `sonar-project.properties`).

À la racine du projet :

```bash
export SONAR_TOKEN="votre_token"

mvn clean test -Dtest="fr.cnrs.opentheso.v2.**" sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

Module `v2/setting` uniquement :

```bash
mvn clean test -Dtest="fr.cnrs.opentheso.v2.setting.**" sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

Rapport JaCoCo HTML : `target/site/jacoco/index.html`

## IntelliJ IDEA

1. Installer le plugin **SonarQube for IDE**
2. **Settings** → **Tools** → **SonarQube** → ajouter `http://localhost:9000` + token
3. **SonarQube** tool window → **Bind to SonarQube** → projet `opentheso`

## Arrêter

```bash
cd docker/sonarqube
docker compose down
```

Pour supprimer aussi les données : `docker compose down -v`
