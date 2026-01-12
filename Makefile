.PHONY: help build docker-build docker-up docker-down clean run stop logs ps

# Variables
SERVICE_A_NAME=sensor-client
SERVICE_B_NAME=sensor-server
SERVICE_A_IMAGE=$(SERVICE_A_NAME):latest
SERVICE_B_IMAGE=$(SERVICE_B_NAME):latest

# Default target
help:
	@echo "=== Sensor Reactive System - Microservices ==="
	@echo ""
	@echo "Build & Development:"
	@echo "  make build-service-a     - Build Service A (Client) Maven project"
	@echo "  make build-service-b     - Build Service B (Server) Maven project"
	@echo "  make build               - Build both services"
	@echo "  make clean               - Clean build artifacts and Docker images"
	@echo ""
	@echo "Docker Management:"
	@echo "  make docker-build        - Build Docker images for both services"
	@echo "  make docker-up           - Start all services with Docker Compose"
	@echo "  make docker-down         - Stop and remove Docker containers"
	@echo "  make logs-a              - View Service A logs"
	@echo "  make logs-b              - View Service B logs"
	@echo "  make logs-db             - View PostgreSQL logs"
	@echo "  make logs                - View all logs"
	@echo "  make ps                  - List running Docker containers"
	@echo ""
	@echo "Utilities:"
	@echo "  make help                - Show this help message"
	@echo "  make test-endpoints      - Test API endpoints"
	@echo "  make health              - Check services health"
	@echo ""

# Build Maven projects
build-service-a:
	@echo "Building Service A (Client)..."
	cd service-a && mvn clean package -DskipTests
	@echo "Service A build completed!"

build-service-b:
	@echo "Building Service B (Server)..."
	cd service-b && mvn clean package -DskipTests
	@echo "Service B build completed!"

build: build-service-a build-service-b
	@echo "Both services built successfully!"

# Build Docker images
docker-build:
	@echo "Building Docker images for both services..."
	docker-compose build
	@echo "Docker images built successfully!"

# Start Docker Compose
docker-up:
	@echo "Starting Docker Compose with all services..."
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 5
	@docker-compose ps
	@echo ""
	@echo "Services started!"
	@echo "  Service A (Client):  http://localhost:8080"
	@echo "  Service B (Server):  http://localhost:8081"
	@echo "  PostgreSQL:          localhost:9432"

# Stop Docker Compose
docker-down:
	@echo "Stopping Docker Compose..."
	docker-compose down -v
	@echo "Docker services stopped!"

# View Docker logs
logs-a:
	docker-compose logs -f service-a

logs-b:
	docker-compose logs -f service-b

logs-db:
	docker-compose logs -f postgres

logs:
	docker-compose logs -f

# List running containers
ps:
	docker-compose ps

# Stop running containers
stop:
	@echo "Stopping containers..."
	docker-compose stop
	@echo "Containers stopped!"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts and Docker resources..."
	cd service-a && mvn clean 2>/dev/null || true
	cd service-b && mvn clean 2>/dev/null || true
	docker-compose down -v 2>/dev/null || true
	docker rmi $(SERVICE_A_IMAGE) $(SERVICE_B_IMAGE) 2>/dev/null || true
	rm -rf service-a/target/ service-b/target/
	@echo "Clean completed!"

# Run applications locally
run:
	@echo "Starting PostgreSQL..."
	docker-compose up -d postgres
	@echo "Waiting for PostgreSQL..."
	@sleep 3
	@echo "Note: To run services locally, start them separately:"
	@echo "  Terminal 1: cd service-b && mvn spring-boot:run"
	@echo "  Terminal 2: cd service-a && mvn spring-boot:run"

# Check if PostgreSQL is running
check-db:
	@echo "Checking PostgreSQL connection..."
	docker-compose exec postgres pg_isready -U postgres || echo "PostgreSQL is not running"

# Test sensor endpoints
test-endpoints:
	@echo "Testing sensor endpoints..."
	@echo ""
	@echo "1. Testing Service B (Server) - Single Sensor Stream:"
	@echo "   http://localhost:8081/api/sensors/stream?sensorId=1&limit=3"
	@curl -s "http://localhost:8081/api/sensors/stream?sensorId=1&limit=3" | head -3
	@echo ""
	@echo ""
	@echo "2. Testing Service A (Client) - Proxy Stream:"
	@echo "   http://localhost:8080/api/client/sensors?sensorId=1&limit=3"
	@curl -s "http://localhost:8080/api/client/sensors?sensorId=1&limit=3" | head -3
	@echo ""

# Health check
health:
	@echo "Checking services health..."
	@echo ""
	@echo "Service A (Client):"
	@curl -s http://localhost:8080/actuator/health | python -m json.tool || echo "  Not responding"
	@echo ""
	@echo "Service B (Server):"
	@curl -s http://localhost:8081/actuator/health | python -m json.tool || echo "  Not responding"

# Reset database
reset-db:
	@echo "Resetting database..."
	docker-compose down -v
	docker-compose up -d postgres
	@echo "Database reset completed!"
