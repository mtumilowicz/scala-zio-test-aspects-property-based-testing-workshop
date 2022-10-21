package app

import zio.{Task, ZIO}

object AccountService {
  def createAccount(account: Account): Task[AccountCreated] = ZIO.succeed(AccountCreated(account.id))
}
