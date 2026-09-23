COMPOSE := docker compose -f docker/Docker-compose.yml

.PHONY: stack-up stack-down stack-reset seed-users

stack-up: docker/.env          ## build and start the stack
	$(COMPOSE) up -d --build

stack-down: docker/.env        ## stop the stack, keep the database
	$(COMPOSE) down

stack-reset: docker/.env       ## drop the database and start clean
	$(COMPOSE) down -v
	$(MAKE) stack-up

seed-users: docker/.env        ## create the .env accounts in the running stack
	set -a; . ./docker/.env; set +a; \
	$(COMPOSE) exec -T opentheso-db psql -U "$$DB_USER" -d "$$DB_NAME" \
	  -v admin_user="$$SEED_ADMIN_USER"          -v admin_pass="$$SEED_ADMIN_PASSWORD" \
	  -v padmin_user="$$SEED_PROJECT_ADMIN_USER" -v padmin_pass="$$SEED_PROJECT_ADMIN_PASSWORD" \
	  -v manager_user="$$SEED_MANAGER_USER"      -v manager_pass="$$SEED_MANAGER_PASSWORD" \
	  -v contrib_user="$$SEED_CONTRIBUTOR_USER"  -v contrib_pass="$$SEED_CONTRIBUTOR_PASSWORD" \
	  -v project="$$SEED_PROJECT" \
	  < docker/seed-users.sql

docker/.env:
	cp docker/.env.example $@
