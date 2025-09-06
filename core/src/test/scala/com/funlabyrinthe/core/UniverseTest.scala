package com.funlabyrinthe.core

class UniverseTest extends munit.FunSuite:
  import UniverseTest.*

  test("module resolution") {
    import Universe.resolveModuleDependencies as resolve
    import FakeModules.*

    assertEquals(
      List(Core, A, AdditionalComponents),
      resolve(Set(A))
    )
    assertEquals(
      List(Core, A, AdditionalComponents),
      resolve(Set(A, Core))
    )
    assertEquals(
      List(Core, A, D, E, G, B, F, C, AdditionalComponents),
      resolve(Set(A, B, C, D, E, F, G))
    )

    intercept[IllegalArgumentException] {
      resolve(Set(A, B))
    }
    intercept[IllegalArgumentException] {
      resolve(Set(H, I))
    }
  }
end UniverseTest

object UniverseTest:
  private object FakeModules:
    object A extends Module

    object B extends Module:
      override protected def dependsOn: Set[Module] = Set(D)

    object C extends Module:
      override protected def dependsOn: Set[Module] = Set(E, F)

    object D extends Module

    object E extends Module

    object F extends Module:
      override protected def dependsOn: Set[Module] = Set(A)

    object G extends Module

    object H extends Module:
      override protected def dependsOn: Set[Module] = Set(I)

    object I extends Module:
      override protected def dependsOn: Set[Module] = Set(H)
  end FakeModules
end UniverseTest
