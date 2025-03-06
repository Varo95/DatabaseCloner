# DataBase Clone
This is a project for clone database from remote to local

# What do you need
Maybe...Nothing, just the artifact to download and run it from your Windows, Linux or Mac OS

# Features
- Create a docker container to storage the cloned database
- Clone Oracle Database
- Clone MySQL Database (in progress)
- Clone PostgreSQL Database (soon)
- Clone SQL Server Database (soon)


# What exactly clones?
On Oracle:
- Tables
- Views
- Sequences
- Primary Keys
- Foreign Keys
- Comments on tables and columns
...need more stuff? Just ask for it!

# How it works
Just run it. If Alert Windows appears, click on "Write XML". The program will exit.
Modify The XML (database.xml) file with login data and docker params.
Then run the program again and select 'Origin' and 'Target' database, this will get the data of origin and
'paste' in the target database. Make sure that you got DBA (user privileges) on target database.

(I will make a 'no DBA' version if this repository got some stars)


# How to run Oracle Database on your local machine
First, docker desktop installed and running.
Then you must have oracle account accepting their conditions. Feel free to register here: https://container-registry.oracle.com/

To accept their conditions, at the page provide before, click on Database->enterprise

At this point, on the right side of the page, it will show the command pull, above the command, the terms are there.
Select your language, read and accept the terms.

As this actual date (20/02/2024) Oracle shows a message that you couldn't login to your account using docker or pod login
with your regular password. Then you must create an auth token. To do this, go to the top of the webpage, on your email address
click there, then click on 'Auth Token', create a new token and use it to login on docker instead of your regular password.

## Configuration
This data must be on the XML at this place(Put your credentials on safe place and do not share database.xml file if not really confident their purposes)
```xml
<DockerParams>
    <User username="your@email.com" password="yourSecretPassword"/>
</DockerParams>
```

## Versions
Oracle containers versions are available at the end of the 'enterprise' page (mentioned before)

Actually are available:
- 21.3.0.0 (latest)
- 19.19.0.0
- 19.3.0.0
- 12.2.0.1
- 12.2.0.1-slim

# Developers Note:
- Compiling .exe and .msi on Windows 11 Pro-> Success, no failures
- Compiling .deb on Debian 12-> We need install binutils and fakeroot to compile .deb
- Compiling .dmg on Mac OS -> Success, no failures