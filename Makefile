DOCKER = docker
COMPOSE = ${DOCKER} compose

COMPOSE_OPTIONS = --remove-orphans

SERVICE_NAME = stella-typechecker

run:
	${COMPOSE} run ${COMPOSE_OPTIONS} ${SERVICE_NAME}

.PHONY: run
