# AidBridge Database SQL Scripts

> PostgreSQL 15+ · 22 tables · 16 ENUMs · ~70 indexes

## Quick Start

```bash
# Create database
createdb aidbridge

# Run full migration
psql -d aidbridge -f 000_full_migration.sql

# Or run individual files
psql -d aidbridge -f 001_extensions.sql
psql -d aidbridge -f 002_enums.sql
psql -d aidbridge -f 003_tables.sql
psql -d aidbridge -f 004_indexes.sql
psql -d aidbridge -f 005_triggers.sql
psql -d aidbridge -f 006_seed_data.sql
```

## File Structure

| File                     | Description                                |
| ------------------------ | ------------------------------------------ |
| `000_full_migration.sql` | Master script - runs all files in order    |
| `001_extensions.sql`     | PostgreSQL extensions (uuid-ossp, citext)  |
| `002_enums.sql`          | 16 ENUM type definitions                   |
| `003_tables.sql`         | 22 table definitions with constraints      |
| `004_indexes.sql`        | ~70 index definitions                      |
| `005_triggers.sql`       | Trigger functions (auto updated_at, stats) |
| `006_seed_data.sql`      | Initial data (categories, config, admin)   |
| `999_drop_all.sql`       | Drop everything (development only!)        |

## Tables by Domain

### Auth (2 tables)

- `users` - Central user authentication
- `refresh_tokens` - JWT refresh tokens

### Profiles (2 tables)

- `volunteer_profiles` - Volunteer-specific data
- `sponsor_profiles` - Sponsor stats & badges

### Infrastructure (4 tables)

- `hubs` - Distribution centers
- `hub_staff` - Staff assignments
- `shelters` - Temporary housing
- `system_config` - Key-value config

### Catalog & Inventory (4 tables)

- `item_categories` - Hierarchical item catalog
- `hub_accepted_categories` - Hub-category mapping
- `hub_inventories` - Current stock levels
- `inventory_logs` - Audit trail

### Requests (3 tables)

- `sos_requests` - Emergency rescue requests
- `aid_requests` - Supply delivery requests
- `aid_request_items` - Items in aid requests

### Donations (2 tables)

- `donations` - Sponsor donations
- `donation_items` - Items in donations

### Missions (2 tables)

- `missions` - Rescue/delivery tasks
- `dispatch_attempts` - Volunteer dispatch tracking

### Communication (3 tables)

- `chat_messages` - Mission chat
- `ratings` - Post-mission feedback
- `notifications` - Push notifications

## ENUM Types

| ENUM                        | Values                                    |
| --------------------------- | ----------------------------------------- |
| `user_role`                 | VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN  |
| `hub_status`                | ACTIVE, INACTIVE, EMERGENCY               |
| `shelter_status`            | ACTIVE, INACTIVE, FULL                    |
| `urgency_level`             | CRITICAL, HIGH, MEDIUM, LOW               |
| `sos_status`                | PENDING → COMPLETED/CANCELLED             |
| `aid_status`                | PENDING → COMPLETED/CANCELLED             |
| `donation_status`           | REGISTERED → RECEIVED/REJECTED            |
| `mission_type`              | RESCUE, DELIVERY                          |
| `mission_status`            | PENDING → COMPLETED/CANCELLED             |
| `dispatch_response`         | PENDING, ACCEPTED, REJECTED, TIMEOUT      |
| `badge_level`               | BRONZE, SILVER, GOLD, PLATINUM            |
| `vehicle_type`              | MOTORBIKE, CAR, BICYCLE, WALKING          |
| `otp_type`                  | REGISTER, FORGOT_PASSWORD, VERIFY_PHONE   |
| `inventory_change_type`     | DONATION_IN, MISSION_OUT, ADJUSTMENT, ... |
| `message_type`              | TEXT, IMAGE                               |
| `notification_related_type` | MISSION, DONATION, SOS_REQUEST, ...       |

## Key Constraints

- `users`: email OR phone required
- `hub_staff`: unique active assignment per hub
- `hub_inventories`: unique (hub_id, item_category_id)
- `missions`: RESCUE ↔ sos_request_id, DELIVERY ↔ aid_request_id + hub_id
- `chat_messages`: TEXT xor IMAGE content
- `ratings`: unique per mission, score 1-5

## Default Admin

After migration, default admin account:

- Email: `admin@aidbridge.vn`
- Password: `Admin@123` (change immediately!)
