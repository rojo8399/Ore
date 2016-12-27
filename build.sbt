name := "ore"

version := "1.0.2"

lazy val `ore` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(cache, ws, specs2 % Test)

unmanagedResourceDirectories in Test <+=  baseDirectory (_ /"target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

// Additional dependencies
resolvers ++= Seq(
  "sponge" at "https://repo.spongepowered.org/maven"
)

libraryDependencies ++= Seq(
  "org.spongepowered"     %%  "sponge-play"             %   "1.0.0-SNAPSHOT",
  "org.spongepowered"     %%  "play-discourse"          %   "1.0.0-SNAPSHOT",
  "org.spongepowered"     %   "plugin-meta"             %   "0.2",
  "com.typesafe.play"     %%  "play-slick"              %   "2.0.0",
  "com.typesafe.play"     %%  "play-slick-evolutions"   %   "2.0.0",
  "org.postgresql"        %   "postgresql"              %   "9.4.1212.jre7",
  "com.github.tminglei"   %%  "slick-pg"                %   "0.12.0",
  "org.apache.commons"    %   "commons-io"              %   "1.3.2",
  "org.pegdown"           %   "pegdown"                 %   "1.6.0",
  "com.getsentry.raven"   %   "raven-logback"           %   "7.2.2",
  "org.bouncycastle"      %   "bcpg-jdk15on"            %   "1.55"
)
