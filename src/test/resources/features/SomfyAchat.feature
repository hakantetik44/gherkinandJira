@test
Feature: Accepter les cookies sur Somfy
  En tant qu'utilisateur
  Je veux accepter les cookies
  Afin de naviguer sur le site

  @TestCaseKey=SMF2-1 @TestPlanKey=SMF2-2
  Scenario: Accepter les cookies et naviguer vers Produits
    Given je suis sur la page d'accueil de Somfy
    When je clique sur le bouton "TOUT ACCEPTER"
    Then le bandeau des cookies disparaît
    When je clique sur le menu "Produits"