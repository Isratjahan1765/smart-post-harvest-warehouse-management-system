# Smart Post-Harvest Warehouse Management System (SPHWMS)

A web application that streamlines post-harvest warehouse operations: product management, arrival tracking, inventory monitoring, driver management, revenue reporting and surplus food donation.

## Features
- Product catalogue (add, update, search, filter by category)
- Dashboard with live summaries and monthly arrivals chart
- Record and confirm new arrivals (Pending / Confirmed)
- Inventory monitoring with stock status and audit log
- Driver records, driver login and own-status update
- Revenue and monthly reports with XLSX export
- Surplus food donation to NGOs
- English / Bangla language switch
- Secure login with role-based access control

## User Roles
Administrator, Warehouse Manager / Staff, Driver, Management / Report Viewer, Donation Coordinator

## Technology Used
- Back end: Java, Spring Boot (Spring MVC), Spring Security, Spring Data JPA
- Front end: HTML, CSS
- Database: MySQL

## How to Run
1. Clone the repository
2. Create a MySQL database named `sphwms`
3. Set your database username and password in `application.properties`
4. Run the application with `./mvnw spring-boot:run`
5. Open `http://localhost:8080`

## Team Members
| Name | Student ID | GitHub |
|---|---|---|
| Mst. Israt Jahan | 2230968 | Isratjahan1765 |
| Fahrin Kabir Sehrin | 2211099 | |
| Sadiyah Sultana Nishat | 2210020 | |
| Bandhan Karmakar | 2111522 | |

## Supervisor
Dr. Md. Mushfique Anwar, Independent University, Bangladesh (CSE-451)

## Documentation
See the [Wiki](https://github.com/Isratjahan1765/smart-post-harvest-warehouse-management-system/wiki)
