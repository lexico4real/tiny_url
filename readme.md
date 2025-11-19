# URL Shortener Service

A high-performance, RESTful URL shortener microservice built with Spring Boot that converts long URLs into short, shareable links with analytics and monitoring capabilities.

## Features

- **URL Shortening**: Convert long URLs to short codes (Base62 encoding)
- **Smart Redirection**: HTTP 302 redirects with hit tracking
- **URL Metadata**: Access creation time, expiry, and usage statistics
- **Collision-safe**: Automatic retry logic for code generation
- **Expiration Support**: Optional TTL for short URLs
- **Rate Limiting**: Protection against API abuse (10 requests/minute per IP)
- **Comprehensive Metrics**: Spring Boot Actuator with custom counters
- **Validation**: Full request validation with meaningful error messages
- **Idempotent Operations**: Same long URL returns same short code
- **API Documentation**: OpenAPI 3 + Swagger UI

## 🛠 Tech Stack

- **Java 21** - Runtime environment
- **Spring Boot 3.2** - Application framework
- **Spring Data JPA** - Data persistence
- **H2/PostgreSQL** - Database (H2 for development, PostgreSQL for production)
- **Bucket4j** - Rate limiting
- **Micrometer** - Application metrics
- **Testcontainers** - Integration testing
- **Maven** - Build tool
- **Docker** - Containerization

## Prerequisites

- Java 21 or later
- Maven 3.6+
- Docker (optional, for PostgreSQL)

## 🏃‍♂️ Quick Start

### Local Development (H2 Database)

```bash
# Clone the repository
git clone <repository-url>
cd tiny_url

# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/tiny_url-0.0.1-SNAPSHOT.jar
```

### With Docker Compose (PostgreSQL)

```bash
# Start PostgreSQL and the application
docker-compose up --build

# Or run in background
docker-compose up -d
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test categories
./mvnw test -Dtest="*Test"           # Unit tests only
./mvnw test -Dtest="*IntegrationTest" # Integration tests only
./mvnw test -Dtest="*RepositoryTest"  # Repository tests only

# Run with coverage report
./mvnw test jacoco:report
```

## API Documentation

Once running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **Actuator Endpoints**: http://localhost:8080/actuator
- **H2 Console** (dev): http://localhost:8080/h2-console

## API Endpoints

### Create Short URL
```http
POST /api/urls
Content-Type: application/json

{
  "longUrl": "https://example.com/very-long-path",
  "expiryDays": 30
}
```

**Response:**
```json
{
  "code": "abc123",
  "shortUrl": "http://localhost:8080/r/abc123",
  "longUrl": "https://example.com/very-long-path"
}
```

### Redirect to Long URL
```http
GET /r/{code}
```

**Response:** HTTP 302 Redirect to the original URL

### Get URL Metadata
```http
GET /api/urls/{code}
```

**Response:**
```json
{
  "code": "abc123",
  "longUrl": "https://example.com/very-long-path",
  "createdAt": "2024-01-15T10:30:00Z",
  "expiresAt": "2024-02-14T10:30:00Z",
  "hitCount": 42
}
```

## Configuration

### Application Properties

```yaml
# src/main/resources/application.yml
app:
  short-url:
    base-url: http://localhost:8080
  url:
    default-expiry-days: 30
    max-length: 2048

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

### Environment Variables

| Variable | Description | Default |
|-|-||
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:h2:mem:testdb` |
| `APP_SHORT_URL_BASE_URL` | Base URL for short links | `http://localhost:8080` |
| `APP_URL_DEFAULT_EXPIRY_DAYS` | Default URL expiry | `30` |

## 🗄 Data Model

```java
UrlMapping {
  id: Long
  code: String (unique, indexed, max 10 chars)
  longUrl: String (max 2048 chars)
  createdAt: Instant
  expiresAt: Instant? (nullable)
  hitCount: Long
}
```

## Testing Strategy

### Test Types
- **Unit Tests**: Service layer, code generation, validation
- **Repository Tests**: Data access layer with @DataJpaTest
- **Web MVC Tests**: Controller layer with mocked dependencies
- **Integration Tests**: Full end-to-end with Testcontainers

### Test Coverage
- Code generation with collision handling
- URL validation and error scenarios
- Database constraints and transactions
- Rate limiting behavior
- Redirect functionality

## Docker Deployment

### Build Docker Image
```bash
docker build -t tiny-url-service .
```

### Docker Compose
```yaml
version: '3.8'
services:
  tiny-url:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/shortener
    depends_on:
      - postgres

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=shortener
      - POSTGRES_USER=shortener
      - POSTGRES_PASSWORD=shortener
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

## Monitoring & Metrics

### Custom Metrics
- `shortener.redirect.total`: Total redirect counter by status

### Actuator Endpoints
- `/actuator/health`: Application health
- `/actuator/metrics`: Application metrics
- `/actuator/info`: Application information

## Rate Limiting

- **10 requests per minute** per IP address for POST endpoints
- Implemented using Bucket4j with in-memory storage
- Returns HTTP 429 Too Many Requests when limit exceeded

## Design Decisions & Trade-offs

### Architecture
- **Layered Architecture**: Clear separation of concerns (Controller → Service → Repository)
- **RESTful Design**: Standard HTTP semantics and status codes
- **Idempotent Operations**: Same long URL returns same short code to prevent duplication

### Code Generation
- **Base62 Encoding**: 6-character codes (62^6 ≈ 56 billion combinations)
- **Random Generation**: Privacy-focused (not sequential)
- **Collision Handling**: Automatic retry with new random codes

### Data Persistence
- **JPA/Hibernate**: Object-relational mapping for productivity
- **H2 (Development)**: Fast in-memory database for local development
- **PostgreSQL (Production)**: Robust production-grade database
- **Indexed Fields**: Optimized queries on `code` and `longUrl`

### Trade-offs
- **In-Memory Rate Limiting**: Simple implementation, not distributed (OK for single instance)
- **No Authentication**: MVP focus, can be added later
- **Simple Expiry**: Basic TTL, no background cleanup job

## 🚧 Future Enhancements

- [ ] User authentication and personalized URLs
- [ ] Custom short code support
- [ ] Bulk URL shortening
- [ ] Advanced analytics (referrers, geolocation)
- [ ] Redis integration for caching
- [ ] Background expiry cleanup job
- [ ] QR code generation
- [ ] API key authentication

## API Examples

### Using cURL

**Create Short URL:**
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com/very-long-url"}'
```

**Redirect:**
```bash
curl -I http://localhost:8080/r/abc123
```

**Get Metadata:**
```bash
curl http://localhost:8080/api/urls/abc123
```

## Troubleshooting

### Common Issues

**H2 Database Connection:**
- Ensure H2 console is enabled in development
- Check JDBC URL format

**PostgreSQL Connection:**
- Verify Docker container is running
- Check environment variables

**Test Failures:**
- Ensure Docker is running for integration tests
- Check test-specific application properties

### Logs
```bash
# View application logs
tail -f logs/application.log

# Enable debug logging
./mvnw spring-boot:run -Dlogging.level.com.oluyinkabright.tiny_url=DEBUG
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request
