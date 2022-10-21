package app

import enumeratum._

sealed trait AccountStatus extends EnumEntry

object AccountStatus extends Enum[AccountStatus] {

  case object AccountLocked extends AccountStatus

  case object AccountUnlocked extends AccountStatus

  override def values: IndexedSeq[AccountStatus] = findValues
}