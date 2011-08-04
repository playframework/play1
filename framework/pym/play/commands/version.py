
COMMANDS = ['version']

HELP = {
    'version': 'Print the framework version'
}

def execute(**kargs):
    version = kargs.get("version")
    showLogo = kargs.get("showLogo")

    # If we've shown the logo, then the version has already been printed
    if not showLogo:
        print version
