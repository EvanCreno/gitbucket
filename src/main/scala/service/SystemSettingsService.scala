package service

import util.Directory._
import SystemSettingsService._

trait SystemSettingsService {

  def saveSystemSettings(settings: SystemSettings): Unit = {
    val props = new java.util.Properties()
    props.setProperty(AllowAccountRegistration, settings.allowAccountRegistration.toString)
    props.setProperty(Gravatar, settings.gravatar.toString)
    props.setProperty(Notification, settings.notification.toString)
    if(settings.notification) {
      settings.smtp.foreach { smtp =>
        props.setProperty(SmtpHost, smtp.host)
        smtp.port.foreach(x => props.setProperty(SmtpPort, x.toString))
        smtp.user.foreach(props.setProperty(SmtpUser, _))
        smtp.password.foreach(props.setProperty(SmtpPassword, _))
        smtp.ssl.foreach(x => props.setProperty(SmtpSsl, x.toString))
      }
    }
    props.store(new java.io.FileOutputStream(GitBucketConf), null)
  }


  def loadSystemSettings(): SystemSettings = {
    val props = new java.util.Properties()
    if(GitBucketConf.exists){
      props.load(new java.io.FileInputStream(GitBucketConf))
    }
    SystemSettings(
      getValue(props, AllowAccountRegistration, false),
      getValue(props, Gravatar, true),
      getValue(props, Notification, false),
      if(getValue(props, Notification, false)){
        Some(Smtp(
          getValue(props, SmtpHost, ""),
          getOptionValue(props, SmtpPort, Some(25)),
          getOptionValue(props, SmtpUser, None),
          getOptionValue(props, SmtpPassword, None),
          getOptionValue[Boolean](props, SmtpSsl, None)))
      } else {
        None
      }
    )
  }

}

object SystemSettingsService {
  import scala.reflect.ClassTag

  case class SystemSettings(
    allowAccountRegistration: Boolean,
    gravatar: Boolean,
    notification: Boolean,
    smtp: Option[Smtp]
  )
  case class Smtp(
    host: String,
    port: Option[Int],
    user: Option[String],
    password: Option[String],
    ssl: Option[Boolean])

  private val AllowAccountRegistration = "allow_account_registration"
  private val Gravatar = "gravatar"
  private val Notification = "notification"
  private val SmtpHost = "smtp.host"
  private val SmtpPort = "smtp.port"
  private val SmtpUser = "smtp.user"
  private val SmtpPassword = "smtp.password"
  private val SmtpSsl = "smtp.ssl"

  private def getValue[A: ClassTag](props: java.util.Properties, key: String, default: A): A = {
    val value = props.getProperty(key)
    if(value == null || value.isEmpty) default
    else convertType(value).asInstanceOf[A]
  }

  private def getOptionValue[A: ClassTag](props: java.util.Properties, key: String, default: Option[A]): Option[A] = {
    val value = props.getProperty(key)
    if(value == null || value.isEmpty) default
    else Some(convertType(value)).asInstanceOf[Option[A]]
  }

  private def convertType[A: ClassTag](value: String) = {
    val c = implicitly[ClassTag[A]].runtimeClass
    if(c == classOf[Boolean])  value.toBoolean
    else if(c == classOf[Int]) value.toInt
    else value
  }

}
