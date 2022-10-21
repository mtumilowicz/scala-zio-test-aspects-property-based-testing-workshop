package app

import enumeratum.{Enum, EnumEntry}
import eu.timepit.refined.types.string.NonEmptyString

import java.util.UUID
case class Account(id: AccountId,
                   status: AccountStatus,
                   description: NonEmptyString)

case class AccountCreated(id: AccountId)
