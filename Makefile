DOCKER = docker
COMPOSE = ${DOCKER} compose

COMPOSE_OPTIONS = --remove-orphans
REBUILD_OPTION = --build

SERVICE_NAME = stella-typechecker

run:
	${COMPOSE} run ${COMPOSE_OPTIONS} ${SERVICE_NAME}

run-rebuilt:
	${COMPOSE} run ${COMPOSE_OPTIONS} ${REBUILD_OPTION} ${SERVICE_NAME}

.PHONY: run run-rebuilt
