import asyncio
import shutil
import subprocess
import sys

from os import chdir, environ


# First, clone the repo
print("\n=> Cloning repo\n")
subprocess.run(["git", "clone", "https://gitlab.com/gserv.me/Saltcord.git", "Saltcord"])

# Swap directories to Saltcord
print("\n=> Moving directory and setting up sys.path")

chdir("Saltcord")
sys.path.append(".")

# Now we can import and set up the Salt API class

print("=> Setting up Salt API")

from bot.salt import SaltAPI
from bot.salt.enums import AuthTypes

eauth = environ["SALTAPI_EAUTH"]
password = environ["SALTAPI_PASS"]
url = environ["SALTAPI_URL"]
user = environ["SALTAPI_USER"]

salt = SaltAPI(
    user, password, url, AuthTypes.from_string(eauth)
)


# Salt API makes use of asyncio, so let's set up a coroutine for it and run it

async def run():
    print("=> Logging into Salt")

    await salt.setup()
    await salt.login()

    print("=> Applying state\n")

    result = await salt.apply_state("shimmer.gserv.me", "docker/kotdis-bot")
    jid = result[0]["jid"]

    print(f"\n=> State applied, job ID: {jid}\n")

    await salt.teardown()


asyncio.get_event_loop().run_until_complete(run())

# Finally, remember to clean up after yourself!
print(f"=> Removing cloned repo")

chdir("..")
shutil.rmtree("Saltcord", ignore_errors=True)
