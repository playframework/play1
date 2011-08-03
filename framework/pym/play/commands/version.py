
COMMANDS = ['version']

HELP = {
    'version': 'Print the framework version'
}

def execute(**kargs):
    env = kargs.get("env")
    showLogo = kargs.get("showLogo")

    # If we've shown the logo, then the version has already been printed
    if not showLogo:
        print env["version"]
