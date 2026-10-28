## Devices

`DEV` menu. It is the inventory of network devices that can be audited. At the top are the search (identifier, name
or vendor) and the criticality and status filters; when you choose a device, its details appear on the right.

![Device inventory with the details of the chosen device](dispositivos.png)

### Register a device

1. Click **+ register asset**.
2. Fill in:
   - **identifier:** unique code within the organization, for example `ROS-RTR-01`;
   - descriptive **name**;
   - **device type** (the vendor is filled in from the type);
   - **organization** (the system administrator chooses it; the organization administrator registers in their own);
   - **site** (optional);
   - **criticality:** low, medium, high or critical. It weighs on the risk level and on alerts.
3. Click **Register device**. It is created with active status.

![New device](dispositivo-alta.png)

The new device counts toward the active device limit of the organization's plan.

### Edit, deactivate or reactivate

In the device details: **Edit** changes its data (the organization cannot be changed). **Deactivate** makes it
inactive: it keeps its history, but does not accept new configurations or audits. **Reactivate** enables it again.
Both actions ask for confirmation and every change is recorded in the transaction log with the previous and new
values.

### Device types

Types (for example *Router ISR 4331 · Cisco · IOS-XE*) are a catalog shared by all organizations, and baselines are
defined per type. Only the system administrator registers them, with **+ type**: name, vendor and family or
operating system.
