#Telegram bot sending GitHub events

To add bot open [Telegram](http://telegram.me/git_hub_bot)

To deploy run `mvn clean heroku:deploy`

## Available commands

* `/start` just greeting
* `/stop` remove all subscriptions
* `/help` output help
* `/settings` lists all your subscriptions
* `/add` https://github.com/example [https://github.com/example ...] – add subscription on users
* `/remove` https://github.com/example [https://github.com/example ...] – remove subscription on users
