# Project Features

·  The data should be pulled from http://open-platform.theguardian.com/documentation/search, using JSON format. The articles should include a title, an image and category.

·  The home page should be an infinite scrolling list of the articles (ability to switch from list view to Pinterest-like view). Scrolling down the list should pull the next items from the feed.

·  Tapping an article should open it in a new page with shared element transition of the image.

·  Ability to save the article for offline use. The app should be able to open the offline items without network connection.

·  Ability to Pin items to the home page view using an icon in the article view page. The pinned items should be listed in a horizontal scrolling view on top of the home page list. When pinning an article, the new item should be reflected in the list dynamically, upon going back to the home page.

·  The app should check for new items in the feed and add them to the list every 30 seconds.

·  A background task to check for new items and generate a notification that new items are available. (The notification should be generated when the app is closed or taken to the background)

## Info

Here is project with MVVM architecture and new android architecture components: LiveData, ViewModel, Room, Data Binding, Worker, [Pagin Library see branch with-pagin-library](https://github.com/LevonVardanyan/SoloLearnTest/tree/with-paging-library).
Used patterns: Repository, Builder, Factory, Singlton

## Structure

I have two sources of data Remote from theguardian's api and from database where we saved articles for offline use.
Thats mean there are two interfaces:
```Java
BaseLocalDataSource
and
```Java
BaseRemoteDataSource
They provides Api for working with data. Every local dataSource implements first one, and remote dataSource implements the second one.
Then I have one implementation for LocalDataSource: RoomLocalDataSource and one for remote: RetrofitRemoteDataSource.
This implementations are singlton because must be one instnace of these for all app.
Its better for testability to have one thing with which ViewModel can interact and ask for data or request for submiting data. It's better to make ViewModel independent from choosing data or working on it, he can only ask for data or request for insert it, thats why I have Repository class which will control data transfer. But Repository doesn't know how that data will be got or inserted, he only manages the transfer from dataSource to viewModel. Here Repository have two types of dataSources one local and one remote, but it doesn't contain Implementation classes for those sources, it only have interface references. Then repository will work with Api provided by dataSources (e.g. BaseLocal or BaseRemote).
