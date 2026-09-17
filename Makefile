COMPOSE := docker compose -f docker/Docker-compose.yml

.PHONY: stack-up stack-down stack-reset

stack-up: docker/.env          ## build and start the stack
	$(COMPOSE) up -d --build

stack-down: docker/.env        ## stop the stack, keep the database
	$(COMPOSE) down

stack-reset: docker/.env       ## drop the database and start clean
	$(COMPOSE) down -v
	$(MAKE) stack-up

docker/.env:
	cp docker/.env.example $@
