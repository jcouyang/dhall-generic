let scalaVersion = "2.13"

let Module = { org : Text, name : Text, version : Text }

let Build = { modules : List Module }

let dhallOrg = "org.dhallj"

let dhallVersion = "0.4.0"

in    { version = "0.3"
      , modules =
        [ { org = dhallOrg, name = "dhall-scala", version = dhallVersion }
        , { org = dhallOrg, name = "dhall-scala-codec", version = dhallVersion }
        ]
      }
    : Build
