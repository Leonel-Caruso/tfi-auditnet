## Users and profiles

`USR` menu. It lists all platform users with their organization, profile, status and last access.

![Users, access profiles and permission matrix](usuarios.png)

### Register a user

1. Click **+ user**.
2. Fill in **full name**, **email**, **username** (letters without accents, numbers, dot, hyphen or underscore; it
   cannot be changed later), **organization** and an **initial password** that meets the policy.
3. Check one or more **profiles**. A user can have several, for example organization administrator and analyst.
4. Click **Register user**.

The user receives a welcome email with their username (without the password): share the initial password through a
secure channel.

### Actions on a user

| Action | What it does |
|---|---|
| **Edit** | Changes name, email, organization and profiles. If you change the profile or the organization, their open sessions are closed. |
| **Password** | Assigns a new password, removes any lock and closes their sessions. |
| **Unlock** | Appears when the account was locked after failed attempts. |
| **Disable / Enable** | Logical deletion: they cannot sign in and their sessions are closed, but their history is kept. |

> Nobody can disable themselves or remove their own administrator profile, and there must always be at least one
> active system administrator. Your own password is changed from My account.

### Profiles and permissions

The five profiles are fixed and their permissions are enforced by the services. In **Access profiles** only the
description of each one can be edited. The **Permission matrix** shows what each profile (ADM, ORG, ANL, AUD, RSP)
can do in each module.
