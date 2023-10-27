package app

import eu.timepit.refined.types.string.NonEmptyString

case class Account(id: AccountId,
                   status: AccountStatus,
                   description: NonEmptyString)

case class AccountCreated(id: AccountId)
