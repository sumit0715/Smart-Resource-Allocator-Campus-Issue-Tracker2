# Project Statement

## Problem Statement

Campus issues - broken Wi-Fi, electrical faults, hostel maintenance, damaged furniture and the
like - are usually reported informally, over chat groups or in person. Nothing gets an ID, nothing
gets tracked, and it's easy for a complaint to get lost or forgotten before anyone acts on it.

## Scope

CampusFix covers the full life of a complaint: submission, validation, categorisation, routing to
a department and staff member, status tracking through to resolution and closure, a full history
of every change, and basic analytics for administrators. It does not cover a mobile app, real
SMS/email delivery, or integration with a university ERP - those are listed as future work.

## Target Users

- Students / staff members who report issues
- Maintenance / department staff who resolve them
- Administrators who route complaints and monitor the system

## High-Level Features

- Complaint creation with validation and a generated complaint ID
- Complaint tracking and full status history
- Department / staff assignment (automatic by category, or manual)
- A controlled status lifecycle with invalid transitions rejected
- Complaint resolution with recorded remarks
- Post-resolution feedback
- Admin analytics: counts by status/category/priority, resolution rate, average resolution time
- CSV backups and text reports written via NIO.2, plus an audit log
