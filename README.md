# Treasure Hunt

![Treasure Logo](https://png.pngtree.com/png-vector/20241211/ourmid/pngtree-cute-cartoon-treasure-chest-with-gems-and-coins-clipart-png-image_14728801.png)  

## Description
**Treasure Hunt** is a Minecraft plugin that allows you to create custom treasures across the world.  
Each treasure executes a command when found by a player. Each player can claim each treasure only once.

The plugin is compatible with **multi-server environments** using MySQL, ensuring that treasures claimed on one server appear as found on the others.

---

## Features

- Create treasures that execute custom commands.
- Track which players have found each treasure.
- Multi-server synchronization via MySQL database.
- Administrative commands:
  - `/treasure create <id> <command>` → create a treasure
  - `/treasure delete <id>` → delete a treasure
  - `/treasure completed <id>` → list players who found it
  - `/treasure list` → list all treasures
- In-memory cache system for better performance.

---

## Installation

1. Download the plugin JAR.  
2. Place it in the `plugins` folder of your Spigot/Paper server.  
3. Configure **database** in the plugin:  

```yaml
Database:
  type: "mysql" # or sqlite
  sqlite:
    fileName: "database.db"
  mysql:
    address: "localhost:3306"
    username: "root"
    password: ""
    database: "treasurehunt"
```

4. Restart the server.  

---

## Usage

### Create a treasure
```text
/treasure create example say %player% found a treasure!
```
Click on a block to set the treasure location.  

### Delete a treasure
```text
/treasure delete example
```

### List treasures
```text
/treasure list
```

### Players who found a treasure
```text
/treasure completed example
```

## Screenshots

![Treasure Example 1](https://i.imgur.com/xdR4x2W.png)  
![Treasure Example 2](https://i.imgur.com/LWzN2xa.png)  

---

## Contributing
Contributions are welcome!  
Feel free to open **issues** or submit **pull requests**.

---

## License
MIT License © [Rapha.dev]
