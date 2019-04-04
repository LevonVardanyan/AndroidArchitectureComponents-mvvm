# Task requirements

·  The data should be pulled from [TheGuardianOpenPlatform](http://open-platform.theguardian.com/documentation/search), using JSON format. The articles should include a title, an image and category.

·  The home page should be an infinite scrolling list of the articles (ability to switch from list view to Pinterest-like view). Scrolling down the list should pull the next items from the feed.

·  Tapping an article should open it in a new page with shared element transition of the image.

·  Ability to save the article for offline use. The app should be able to open the offline items without network connection.

·  Ability to Pin items to the home page view using an icon in the article view page. The pinned items should be listed in a horizontal scrolling view on top of the home page list. When pinning an article, the new item should be reflected in the list dynamically, upon going back to the home page.

·  The app should check for new items in the feed and add them to the list every 30 seconds.

·  A background task to check for new items and generate a notification that new items are available. (The notification should be generated when the app is closed or taken to the background)

## Info

Here is project with **MVVM** architecture and new **android architecture components**: **LiveData, ViewModel, Room, Data Binding, Worker**, [Pagin Library see branch with-pagin-library](https://github.com/LevonVardanyan/SoloLearnTest/tree/with-paging-library).
Used patterns: Repository, Builder, Factory, Singlton, Dependency Injection

## Structure

I have two sources of data Remote from theguardian's api and from database where I saved articles for offline use.
Thats mean there are two interfaces:
```Java
BaseLocalDataSource
```
and
```Java
BaseRemoteDataSource
```
They provides Api for working with data. Every local dataSource implements first one, and remote dataSource implements the second one.

Then I have one implementation for BaseLocalDataSource: **RoomLocalDataSource** and one for remote: **RetrofitRemoteDataSource**.
This implementations are singlton because must be one instnace of these for all app.

Its better for testability to have one thing with which ViewModel can interact and ask for data or request for inserting data. For making ViewModel independent from choosing data or working on it, (he can only ask for data or request for insert it), I have **Repository** class which will control data transfer.
But Repository doesn't know how that data will be got or inserted, he only manages the transfer from dataSource to viewModel. Here Repository has two types of dataSources: one local and one remote, but it doesn't contain Implementation classes for those sources, it only have interface references. Then repository will work with Api provided by dataSources (e.g. BaseLocal or BaseRemote).

Also there is class
```Java
RepositoryProvider
```
This has only one method which returns the singlton instnace of Repository class. 

**RoomLocalDataSource** implementation contains the work logic with [Android Room](https://developer.android.com/training/data-storage/room/index.html) framework.

**RetrofitRemoteDataSource** contains work logic with Retrofit library

My **viewModel** is 
```Java
FeedViewModel
```
It manages the connection between View and Data, and saves the app state. It contains LiveData objects for observing by View or Observable objects for using with DataBinding. Also it contains Repository instance for asking data, instance would be passed to ViewModel by constructor, when it would be created by **ViewModelFactory**. 
Also FeedViewModel makes the periodic checking for new Articles by running some Runnable on handler.

Here 
```Java
ViewModelFactory
```
class provides the ViewModel instances.

If you need other view models you can add those viewModels creation in this class, because here we can pass some ojects to view model constructor. Here I passed the instance of Repositroy to FeedViewModel.

For threading I have 
```Java
MyExecutor
```
singlton class, which have 3 instances of Executor, for **UI** thread, for **DB** requests and for **NETWORK** request. Also it has one thread for **periodic** requests with delay. For making requests on some thread you must call 
```Java
MyExecutor.getInstance().lunchOn(LunchOn, Runnable)
```
For RecyclerView paged scroll I have 
```Java
PagedScroll
```
class, which must be created with RecyclerView instance, and PagedScroll.Callback instance which have two methods 
```Java
onLoadMore()
```
and 
```Java
isLoading()
```
You can set **loadingThreshold** integer  and **loadForFirstTime** boolean when you build the PagedScroll.
First parameter defines how many items must be remain for scrolling down before making onLoadMore call. And the second parameter is for calling onLoadMore method one time when PagedScroll instance will be created.
**onLoadMore** method will be called when 
```Java
totalItemCount - visibleItemCount <= firstVisibleItemPosition + this.loadingThreshold
```
or **loadForFirstTime** boolean is true. **isLoading** method must return true if currently you make request and false otherwise
