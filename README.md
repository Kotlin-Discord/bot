KotDis Bot
==========

This repository contains what will eventually be the primary bot used for
managing the Kotlin Discord server.

Right now, it's in a very early state.

Running with Docker
===================

In order to run the bot with Docker and [Docker Compose](https://docs.docker.com/compose/install/), 
You'll need a local copy of our [website](https://github.com/Kotlin-Discord/site).  

Getting the containers ready
----------------------------

In the site project root, you'll need to run
```
npm i
./gradlew build
```

In the bot project root, you'll need to run
```
./gradlew build
```
You'll also need to create an `.env` file in the bot project root
and set the `BOT_TOKEN` variable to your test bot token. 

Additionally, the compose file assumes that both the site, and the bot root folders
are in the same directory. You can override this by setting the variable `SITE_PROJECT_PATH` to the path to 
the site root folder.

Running the bot and the site
----------------------------

Simply type `docker-compose up` in the bot project root to launch both containers.
