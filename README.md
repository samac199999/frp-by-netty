Simple frp applications written in JAVA support forward and forward port mapping and UDP port mapping

1. Server configuration file application.yml
server:
  port: 18086

config:
  host: 0.0.0.0
  port: 53215 # Server listening port
  use-udt: true # Whether to enable UDT
  password: a123456 # Connection password
  write-limit: 0
  read-limit: 0

2. Client configuration file application.yml
config:
  host: 127.0.0.1 # Server IP or domain name
  port: 53215 # Server port
  token: test56165116565 # Client token
  use-udt: false # Whether to use UDT to connect to the server
  password: a123456 # Connection password
  write-limit: 0
  read-limit: 0
  mappings:
    - 5255;127.0.0.1:5244;false # Server mapping port; Local service port; Non-inversion mapping
    - 5253;127.0.0.1:5244;true  # Reverse mapping
  udp-mappings:
    - 5255;127.0.0.1:5244;false

4. samac199999@gmail.com
