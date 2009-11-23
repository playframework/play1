package object play {

    // -- LOGGING

    def trace(msg: String, args: Any*) = play.Logger.trace(msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def debug(e: Throwable, msg: String, args: Any*) = play.Logger.debug(e, msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def debug(msg: String, args: Any*) = play.Logger.debug(msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def info(e: Throwable, msg: String, args: Any*) = play.Logger.info(e, msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def info(msg: String, args: Any*) = play.Logger.info(msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def warn(e: Throwable, msg: String, args: Any*) = play.Logger.warn(e, msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def warn(msg: String, args: Any*) = play.Logger.warn(msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def error(e: Throwable, msg: String, args: Any*) = play.Logger.error(e, msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def error(msg: String, args: Any*) = play.Logger.error(msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def fatal(e: Throwable, msg: String, args: Any*) = play.Logger.fatal(e, msg, args.map(_.asInstanceOf[AnyRef]): _*)
    def fatal(msg: String, args: Any*) = play.Logger.fatal(msg, args.map(_.asInstanceOf[AnyRef]): _*)


    // -- CONFIGURATION

    def configuration = new RichConfiguration(play.Play.configuration)


    // - IMPLICITS

    implicit def withEscape(x: Any) = new WithEscape(x)

}