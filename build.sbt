name := "backend"
 
version := "1.0" 
      
lazy val `backend` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
scalaVersion := "2.11.11"

libraryDependencies ++= Seq( jdbc , cache , ws , specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.16.2"
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.12.0"
)

libraryDependencies += filters