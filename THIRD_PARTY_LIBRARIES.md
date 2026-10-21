# Third-Party Libraries

Every direct dependency Food Seer uses, with its license. Versions match `food-seer-backend/pom.xml` and `food-seer-frontend/package.json` (a `^` means "this version or a newer compatible one"). Transitive dependencies are not listed; see "Regenerating this list" below.

## Backend (Java, Maven)

| Library | Version | License | Used for |
| :- | :- | :- | :- |
| [Spring Boot Starter Web](https://spring.io/projects/spring-boot) | 3.1.4 | Apache-2.0 | REST API |
| [Spring Boot Starter Security](https://spring.io/projects/spring-security) | 3.1.4 | Apache-2.0 | Authentication and role checks |
| [Spring Boot Starter Data JPA](https://spring.io/projects/spring-data-jpa) | 3.1.4 | Apache-2.0 | Database access |
| [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/) | 3.1.0 | EPL-2.0 or EDL-1.0 (BSD-3-Clause) | Entity annotations |
| [JJWT API, Impl, Jackson](https://github.com/jwtk/jjwt) | 0.11.5 | Apache-2.0 | JSON Web Tokens |
| [org.json](https://github.com/stleary/JSON-java) | 20210307 | JSON License | JSON parsing for the chatbot |
| [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) | 8.4.0 | GPL-2.0 with Universal FOSS Exception 1.0 | MySQL driver |
| [Project Lombok](https://projectlombok.org) | 1.18.48 | MIT | Boilerplate generation |
| [Spring Boot Maven Plugin](https://spring.io/projects/spring-boot) | 3.1.4 | Apache-2.0 | Running and packaging the app |

## Backend testing and quality

| Library | Version | License | Used for |
| :- | :- | :- | :- |
| [Spring Boot Starter Test](https://spring.io/projects/spring-boot) | 3.1.4 | Apache-2.0 | Test framework support |
| [Spring Security Test](https://spring.io/projects/spring-security) | 6.1.4 | Apache-2.0 | Testing secured endpoints |
| [JUnit 5 (BOM and Jupiter)](https://junit.org/junit5/) | 5.11.0 | EPL-2.0 | Unit tests |
| [H2 Database](https://www.h2database.com) | 2.2.224 | MPL-2.0 or EPL-1.0 | In-memory database for tests and CI |
| [Selenium Java](https://www.selenium.dev) | 4.38.0 | Apache-2.0 | Browser UI tests |
| [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) | 6.1.0 | Apache-2.0 | ChromeDriver setup for UI tests |
| [JaCoCo Maven Plugin](https://www.jacoco.org) | 0.8.11 | EPL-2.0 | Code coverage |
| [PIT (pitest-maven)](https://pitest.org) | 1.19.4 | Apache-2.0 | Mutation testing |
| [pitest-junit5-plugin](https://github.com/pitest/pitest-junit5-plugin) | 1.2.3 | Apache-2.0 | PIT support for JUnit 5 |
| [Apache Maven core plugins](https://maven.apache.org/plugins/) (clean, resources, compiler, surefire, jar, install, deploy, site, project-info-reports) | see `pom.xml` | Apache-2.0 | Build lifecycle |

## Frontend (JavaScript, npm)

| Library | Version | License | Used for |
| :- | :- | :- | :- |
| [React](https://react.dev) | ^18.2.0 | MIT | UI |
| [React DOM](https://react.dev) | ^18.2.0 | MIT | Rendering |
| [React Router DOM](https://reactrouter.com) | ^6.3.0 | MIT | Page routing |
| [React Scripts (Create React App)](https://github.com/facebook/create-react-app) | 5.0.1 | MIT | Build, dev server, Jest runner |
| [Chart.js](https://www.chartjs.org) | ^4.5.1 | MIT | Charts on the Personal Diet page |
| [react-chartjs-2](https://react-chartjs-2.js.org) | ^5.3.1 | MIT | React wrapper for Chart.js |
| [Web Vitals](https://github.com/GoogleChrome/web-vitals) | ^2.1.4 | Apache-2.0 | Performance metrics |
| [@testing-library/react](https://testing-library.com) | ^13.3.0 | MIT | Component tests |
| [@testing-library/jest-dom](https://testing-library.com) | ^5.16.4 | MIT | DOM assertions |
| [@testing-library/user-event](https://testing-library.com) | ^13.5.0 | MIT | Simulated user input |

## Tools and models

| Tool | Version | License | Used for |
| :- | :- | :- | :- |
| [Ollama](https://ollama.com) | latest | MIT | Local LLM runtime for the chatbot |
| [qwen2.5:1.5b](https://ollama.com/library/qwen2.5:1.5b) | latest | Apache-2.0 | Chatbot model |
| [pytest](https://pytest.org) | any recent | MIT | API test suite in `proj1/tests` (the rest uses the Python standard library) |

## CI (GitHub Actions)

| Action | Version | License |
| :- | :- | :- |
| [actions/checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [setup-node](https://github.com/actions/setup-node), [upload-artifact](https://github.com/actions/upload-artifact) | v4 | MIT |
| [codecov/codecov-action](https://github.com/codecov/codecov-action) | v4 | MIT |
| [dorny/test-reporter](https://github.com/dorny/test-reporter) | v1 | MIT |

## Regenerating this list

When you add or upgrade a dependency, update this file in the same pull request. To see the full tree, including transitive dependencies:

```bash
cd food-seer-backend && mvn dependency:tree
cd food-seer-frontend && npm ls
```
