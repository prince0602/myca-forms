# Patient Forms Service

## Overview

The `patient-forms` service is a Spring Boot microservice for managing patient-related forms and form templates within the MyCA500 platform.

## Technology Stack

- **Java:** Align with the project’s configured Java version
- **Spring Boot:** 3.2.4 (align the project configuration)
- **Build Tool:** Maven
- **Database:** PostgreSQL (if configured)
- **Version Control:** Bitbucket

## Prerequisites

- Java Development Kit (JDK) compatible with the project
- Apache Maven
- Access to the MyCA500 Maven repository for internal dependencies
- Database access and configuration, if required

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd patient-forms
```

### 2. Configure the application

Update `src/main/resources/application.properties` or `application.yml` with the required application settings, database credentials, and environment-specific configuration.

Do not commit passwords, tokens, or other sensitive credentials.

### 3. Build the application

```bash
mvn clean install
```

### 4. Run tests

```bash
mvn test
```

### 5. Run the application

```bash
mvn spring-boot:run
```

Alternatively, build the JAR and run it:

```bash
mvn clean package
java -jar target/<application-jar-name>.jar
```

## Configuration

Configure the following according to the target environment:

- Application port
- Database connection details, if applicable
- Required internal service URLs
- Authentication and authorization settings
- Environment-specific properties

## Development Guidelines

- Follow the existing Java package and naming conventions.
- Keep business logic in the service layer.
- Use DTOs for API request and response objects where appropriate.
- Add unit and integration tests for new functionality.
- Never commit credentials or environment-specific secrets.

## Contribution Guidelines

1. Create a feature branch for your changes.
2. Follow the team's coding standards.
3. Add or update tests.
4. Submit a pull request for code review.
5. Address review comments before merging.

## Support

For repository access, configuration, or deployment questions, contact the MyCA500 development team or the repository administrator.
