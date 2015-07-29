#Human Computation Portals in PPLib
Human Computation portals are one of the most important elements of PPLib: They allow PPLib to talk to the outside world. 
Currently, there are 2 portals supported: Amazon Mechanical Turk and CrowdFlower. 

The central class when interacting with portals is `HComp`. During runtime, configured portals can be retrieved from that class by calling `HComp(PORTAL_NAME_HERE)` - or for the 2 predefined portals, one can use the convenience methods `HComp.mechanicalTurk` and `HComp.crowdFlower`. 
Note that this class is static and is therefore always present. Also, during runtime, one can query available portals using `HComp.allDefinedPortals` or add another portal using `HComp.addPortal(MY_PORTAL)`. 

Portal initialization happens automatically in the background, when a PPLib application gets loaded

Every portal is a specialization of the class `HCompPortalAdapter` and therefore needs to define a method called `processQuery(query: HCompQuery, properties: HCompQueryProperties)`

## Automatic Portal Initialization
Once Java's Runtime Classloader loads the `HComp` class, it will encounter the method `autoloadConfiguredPortals()`, which will trigger it to scan the whole class path for defined portals and initialze them if it can. 
This is the reason, why a user doesn't need to call `HComp.addPortal()` at any point: The portals get auto-wired once a PPLib application is started based on the data present in a config file. This section's aim is to explain how this works. 

Portals can tie an annotation `@HCompPortal` to themselves, which will make them visible to portal auto loading. 
 This annotation specifies a `HCompPortalBuilder`, with which the portal can be created.  
 
 ```scala
 @HCompPortal(builder = classOf[SimplePortalBuilder], autoInit = true)
 class SimpleHCompPortal(val answerToAnyQuestion: String) extends HCompPortalAdapter {
 //..
 }
 ```
 
 This HCompPortalBuilder will get access to specific parameters of the application config file (We use [TypeSafe Config](https://github.com/typesafehub/config), so the application config file should lie in the classpath of the application and be named `application.conf` ).
 It first needs to tell PPLib which parameters it needs though, which can be done by overriding the `expectedParameters()` method on the builder. Also, on order to actually get configuration parameters, one needs to supply a runtime name for them by overriding `parameterToConfigPath`. 
 Once all of this is done, one can access the requested parameters in the `params(RUNTIME_PARAM_KEY)` method, which will be central for actually creating a new instance of our target portal done by overriding the `build()` method.
  
 ```scala
 object SimpleHCompPortal {
 	val PORTAL_KEY = "simplePortal"
 	val CONFIG_PARAM = "answerToEveryQuestion"
 }
 
 class SimplePortalBuilder extends HCompPortalBuilder {
 	val PARAM_KEY = "answerToEveryQuestion" // we use the same runtime key as the one was named in the config. 
 
 	override def key = RandomHCompPortal.PORTAL_KEY
 
 	override val parameterToConfigPath = Map(PARAM_KEY -> RandomHCompPortal.CONFIG_PARAM)
 
 	override def build: HCompPortalAdapter = new RandomHCompPortal(params(PARAM_KEY))
 
 	override def expectedParameters: List[String] = List(PARAM_KEY)
 }

 ```
 
 Given that this portal has declares the `@HCompPortal` annotation (as shown in the first listing), it will automatically be initialized IF the `application.conf`-file has a directive as follows:
 
 ` hcomp.simplePortal.answerToEveryQuestion = "Hodor" `
 
 ## Configuring MTurk and CrowdFlower
 
 There is an [example application.conf file](https://github.com/pdeboer/PPLib/blob/master/src/main/resources/application.conf_default), that shows the available keys. It can just be copied into the classpath and then be renamed to `application.conf`. Alternatively, one can put the actual credentials in there right away and run `sbt publish-local` to store them in the local repo. 
 
 ```
 hcomp.crowdFlower.apikey = "YOUR KEY"
 hcomp.crowdFlower.sandbox = "true"
 hcomp.mechanicalTurk.accessKeyID = "YOUR KEY"
 hcomp.mechanicalTurk.secretAccessKey = "YOUR KEY"
 hcomp.mechanicalTurk.sandbox = "true"
 hcomp.randomPortal.active = "true"
 ```