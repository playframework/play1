# Play! framework completion 
# put this file in /etc/bash_completion.d/
# or add this in your .bashrc (or .bash_profile) : [ -f <this_script> ] && . <this_script>
# Gaetan Renaudeau <gre@zenexity.com>

### OPTIONS ###

# switch to on to enable play app search
playAppSearch="on" 

# search criteria for valid play app
playapp="app conf public"  

# play options
opts="auto-test build-module check classpath clean dependencies eclipsify evolutions evolutions:apply evolutions:markApplied evolutions:resolve help id idealize install javadoc list-modules modules netbeansify new new-module out pid precompile run restart secret status start stop test war";

#############

# function _isPlayValidApp 
# return 1 if argument $1 is a valid play app directory, else return 0
function _isPlayValidApp {
  local OLDIFS
  OLDIFS=$IFS; # save old IFS to restore it before return.
  IFS=" ";
  if [ -d "$1" ]; then # $1 is a directory
    for subdir in $playapp; do # foreach criteria, check if is a directory
      if [ ! -d "$1/$subdir" ]; then
        IFS=$OLDIFS;
        return 0;
      fi
    done
  fi
  IFS=$OLDIFS;
  return 1;
}

_play() 
{
  local cur prev list argc
  COMPREPLY=()
  
  # variable shortcut
  argc=${COMP_CWORD}
  cur="${COMP_WORDS[argc]}"
  prev="${COMP_WORDS[argc-1]}"

  if [ $argc -eq 1 ] || ([ $argc -eq 2 ] && [ "$prev" == "help" ]) ; then
    ### AutoComplete play options ###
    COMPREPLY=( $(compgen -W "$opts" -- "$cur" ) )
  else
    if ([ $argc -eq 2 ] && [ "$prev" != "new" ] && [ "$prev" != "new-module" ] && [ "$prev" != "list-modules" ] && [ "$prev" != "id" ] && [ "$prev" != "install" ]);

    then
      
      if [ "$playAppSearch" == "on" ]; then
        ### AutoComplete play app ###
        list=""
        OLDIFS=$IFS;
        IFS=$'\n';
        for f in $(find $cur* -maxdepth 2 -type d 2> /dev/null); do 
          _isPlayValidApp "$f";
          # if dir is a play app, add it in the list (and escape space in the path)
          if (($?==1)); then
            list="$list\""$(echo $f | sed 's/ /\\ /g')"\""$'\n'; 
          fi
        done
        IFS=$OLDIFS;
        COMPREPLY=( $(compgen -W "$list" -- "$cur" ) )
      
      else
        ### AutoComplete directory ###
        COMPREPLY=( $(compgen -d -- "$cur" ) )
        
      fi
      
    fi

  fi
}

complete -F _play play

