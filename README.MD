# DataBase Clone
This is a project for clone database from remote to local

# What do you need
Maybe...Nothing, just the artifact to download and run it from your Windows, Linux or Mac OS

# Freatures
- Create a docker container to storage the cloned database
- Clone Oracle Database
- Clone MySQL Database (in progress)
- Clone PostgreSQL Database (soon)
- Clone SQL Server Database (soon)

# How it works
Just run it. If Alert Windows appears, click on "Write XML". The program will exit.
Modify The XML (database.xml) file with login data and docker params.
Then run the program again and select 'Origin' and 'Target' database, this will get the data of origin and
'paste' in the target database. Make sure that you got DBA (user privileges) on target database.
