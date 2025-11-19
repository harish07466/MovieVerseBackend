# 🎬 **MovieVerse Backend – Spring Boot API**

A powerful backend service built using **Java Spring Boot**, designed to provide movie details, metadata, and YouTube trailer information by integrating with external APIs such as **OMDb API** and **YouTube Data API**.
The project also includes **JWT authentication**, user management, and clean modular architecture.

---

## 🚀 **Features**

* 🔐 **JWT Authentication**
* 👤 User Sign-Up, Login & Profile Management
* 🎞 **Movie Search & Details** (via OMDb API)
* 📺 **YouTube Trailer & Video Search** (via YouTube Data API)
* 🌐 Custom WebClient Integration for API Calls
* 📦 Clean Layered Architecture

  * Controllers
  * Services
  * Clients
  * DTOs
  * Entities
  * Configs

---

## 🧱 **Tech Stack**

* **Java 17+**
* **Spring Boot**
* **Spring Security (JWT Auth)**
* **Spring WebClient**
* **OMDb API Integration**
* **YouTube Data API Integration**
* **Maven**
* **MySQL (or any configured DB)**

---

## 📂 **Project Structure**

```
MovieVerseBackEnd/
 ├── Clients/
 │    ├── OmdbClient.java
 │    └── YouTubeClient.java
 ├── Config/
 │    ├── SecurityConfig.java
 │    └── WebClientConfig.java
 ├── Controllers/
 │    ├── AuthenticationController.java
 │    ├── MovieController.java
 │    └── UserController.java
 ├── DTOs/
 │    ├── MovieDetailDto.java
 │    ├── MovieMediaResponse.java
 │    ├── MovieSummaryDto.java
 │    ├── OmdbSearchResponse.java
 │    ├── YouTubeVideosResponse.java
 │    └── YouTubeVideoDto.java
 ├── Entities/
 │    ├── User.java
 │    ├── JWTToken.java
 │    └── OTPVerification.java
 └── MovieVerseBackEndApplication.java
```

---

## ⚙️ **Setup & Installation**

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/harish07466/MovieVerseBackend.git
cd MovieVerseBackend
```

### 2️⃣ Configure Environment Variables

Add your API keys in `application.properties`:

```properties
omdb.api.key=YOUR_OMDB_API_KEY
youtube.api.key=YOUR_YOUTUBE_API_KEY
jwt.secret=YOUR_SECRET_KEY
spring.datasource.url=jdbc:mysql://localhost:3306/movieverse
spring.datasource.username=root
spring.datasource.password=yourpassword
```

### 3️⃣ Run the Application

```bash
mvn spring-boot:run
```

---

## 🔌 **API Endpoints**

### 🔐 Authentication

| Method | Endpoint           | Description                   |
| ------ | ------------------ | ----------------------------- |
| POST   | `/auth/register`   | Register new user             |
| POST   | `/auth/login`      | Login and get JWT             |
| POST   | `/auth/verify-otp` | OTP verification (if enabled) |

---

### 🎬 Movies

| Method | Endpoint                      | Description                   |
| ------ | ----------------------------- | ----------------------------- |
| GET    | `/movies/search?query={name}` | Search movies                 |
| GET    | `/movies/details/{id}`        | Fetch movie details           |
| GET    | `/movies/media/{id}`          | Get poster, ratings, metadata |

---

### 📺 YouTube

| Method | Endpoint                            | Description                |
| ------ | ----------------------------------- | -------------------------- |
| GET    | `/youtube/search?query={movieName}` | Search trailers            |
| GET    | `/youtube/videos/{id}`              | Detailed video information |

---

## 🔐 **Authentication Flow**

1. User registers
2. System generates JWT token after login
3. User sends token in header:

   ```
   Authorization: Bearer <JWT_TOKEN>
   ```

---

## 🛡 **Security**

The backend uses:

* Spring Security
* JWT Token Filters
* Password hashing
* Token-based authentication

---

## 📌 **Future Improvements**

* User watchlist & favorites
* Caching API responses
* Pagination for search results
* Role-based access (ADMIN, USER)

---

## 🤝 **Contributing**

Pull requests are welcome.
For major changes, please open an issue first.

---
