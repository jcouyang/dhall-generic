let Module = { org : Text, name : Text, version : Text }

let Build = { version : Text, modules : List Module }

let dhallOrg = "org.dhallj"

let dhallScalaVersion = "0.4.0"

in    { version = "0.3"
      , modules =
        [ { org = dhallOrg, name = "dhall-scala", version = dhallScalaVersion }
        , { org = dhallOrg
          , name = "dhall-scala-codec"
          , version = dhallScalaVersion
          }
        ]
      }
    : Build
