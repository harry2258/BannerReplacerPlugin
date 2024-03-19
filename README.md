
# A Banners.json replacer for a certain anime game

A simple Banners.json replacer plugin to be used with Grasscutter
## Features

- Auto reload on banner change. `watchGachaConfig` should be doing this but is not.
- Sends a message to all online players when the banner is swapped.
- Updates the "Time Remaining" on the wish to what is set in config.
- Resumes on the last banner, not perfect as it goes by the file number and not the game. (Delete the `state.json` file witin the `/Plugin/ReplaceBannerJSON` folder to start from begining)

## TODO
- Make it so you can toggle to send a message or not in `config.json`.
- Also make the message customizable if the message is toggled.
## Installation

1) Download the latest JAR from the [relases section](https://github.com/harry2258/BannerReplacerPlugin/releases/latest) (Or compile your own).
2) Create a folder called `bannersData` in the same folder as `grasscutter.jar`.
3) Download the banners.json (See [Zhaokugua's](https://github.com/Zhaokugua/Grasscutter_Banners) amazing list for all of them) and place them inside the `bannerData` folder.
4) Run the server and it will create the `config.json` inside `/plugins/ReplaceBannerJSON/` folder
5) Open the `config.json` and set the `ReloadTime` in minutes to how long before the banner is replaced