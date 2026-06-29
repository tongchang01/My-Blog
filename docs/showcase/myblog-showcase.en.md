# MyBlog: A Full-Stack Personal Blog System

For me, a blog is more than a place to publish articles. It is an evolving personal product.

`MyBlog` is a project I actively use and continuously improve. It covers article publishing, content management, comments, albums, short posts, scheduled jobs, permissions, and a music player. In other words, it is closer to a real content platform than a simple blog template.

---

## 1. Project Positioning

This project is split into three parts:

- blog frontend
- admin dashboard
- backend service

This separation makes the system easier to maintain and expand over time.

---

## 2. Technical Architecture

### Frontend

The frontend is divided into two apps:

- `MyBlog-blog`: public-facing blog built with `Vue 3 + TypeScript + Pinia + Element Plus`
- `MyBlog-admin`: admin dashboard built with `Vue 2 + Vuex + Element UI`

The blog focuses on reading experience and presentation, while the admin panel focuses on content operations and management efficiency.

### Backend

The backend is built with:

- Spring Boot
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Quartz
- Knife4j

The project also integrates:

- AWS S3 for object storage
- AWS SES for email delivery

---

## 3. Functional Modules

### Public Blog

The public site currently includes:

- homepage
- article pages
- categories, tags, and archives
- friend links
- albums
- short posts
- comment system
- music player

This is not just a blog homepage. It is designed as a complete content site.

### Admin Dashboard

The admin side supports:

- article management
- category management
- tag management
- comment moderation
- short post management
- album management
- friend link management
- menu management
- role management
- resource permission management
- user management
- online user management
- operation logs and exception logs
- scheduled job management
- website settings
- music management

This is one of the parts I value most, because it turns the project into a usable content platform instead of a simple editor backend.

---

## 4. Custom Features I Added

### Music Player Module

One of the recent improvements is the music player feature:

- a frontend APlayer integration
- admin-side music configuration and track management
- backend music APIs
- database additions for music data and related permissions

I also refined the interaction behavior:

- removed the switch between fixed bottom-right mode and docked floating mode
- kept docked floating mode only
- moved the player upward on desktop to avoid blocking the lower-right UI area

### Website Configuration

The website settings page is not limited to basic text fields. It allows configurable control over:

- website name
- author information
- avatars and logo
- website notice
- social links
- comment review
- email notifications
- reward switch
- music player settings

This gives the site more flexibility without code changes.

### Jobs and Logs

The project also includes:

- Quartz scheduled jobs
- job execution logs
- operation logs
- exception logs

These features make the project feel more like a maintainable system than a one-off side project.

---

## 5. Why I Chose a Three-Part Structure

If the goal were only to publish articles, a single application would be enough.

But I intentionally separated the system into:

- public frontend
- admin frontend
- backend service

because:

- the public site and the admin panel have very different responsibilities
- the architecture is easier to extend
- it reflects a more realistic product structure

---

## 6. What This Project Represents

As a portfolio project, this system demonstrates:

- full-stack project organization
- clear separation between presentation and management layers
- feature growth from “blog” to “content platform”
- engineering concerns such as permissions, logs, scheduled tasks, and configuration
- continuous iteration based on actual usage

It may not be the flashiest project, but it is complete, practical, and continuously evolving.

---

## 7. Screenshot Suggestions

You can insert screenshots in these places:

### Screenshot 1: Blog Homepage

- Capture the public homepage above the fold
- Include navigation, hero/banner area, and article list if possible

```md
![Blog homepage](replace-with-your-image-url)
```

### Screenshot 2: Article Detail Page

- Capture one full article page
- Try to include both content layout and part of the comment section

```md
![Article detail page](replace-with-your-image-url)
```

### Screenshot 3: Admin Dashboard

- Capture the admin homepage or dashboard
- If there are charts or summary cards, include them

```md
![Admin dashboard](replace-with-your-image-url)
```

### Screenshot 4: Article Management Page

- Capture the admin article list page
- Include filters, table, and action buttons if possible

```md
![Article management](replace-with-your-image-url)
```

### Screenshot 5: Website Settings or Music Management

- Prefer the music management page if you want to highlight custom development
- Otherwise, the website settings page is also a good choice

```md
![Website settings or music management](replace-with-your-image-url)
```

---

## 8. Closing

To me, `MyBlog` is not just a blog. It is also a long-term technical work, a management system, and a product I keep refining.

That is exactly why I enjoy building it.
