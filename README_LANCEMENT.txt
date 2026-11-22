SOMMEIL PLUS — VERSION CORRIGÉE

1. Extraire le ZIP.
2. Lire FIREBASE_SETUP.txt et configurer Firebase avant le premier lancement si tu veux
   utiliser les comptes en ligne et la réinitialisation par e-mail.
3. Ouvrir dans Android Studio le dossier qui contient settings.gradle.kts.
4. Vérifier que app/google-services.json est présent, puis faire Gradle Sync.
5. Faire Build > Make Project avant de lancer l'application.
6. Autoriser les notifications et les alarmes exactes sur le téléphone.
7. Dans Alarmes, choisir l'heure du coucher et/ou du réveil, puis Enregistrer.
8. Pour tester « Répéter 5 min », régler une alarme 2 à 3 minutes dans le futur.

Les pages, la navigation, les couleurs et le logo existants sont conservés.
Les pages longues sont défilables. Lorsqu'un clavier apparaît, le contenu reste accessible
en faisant défiler l'écran et la navigation inférieure de l'écran principal se masque.

La validation de l'âge se fait sur la date civile : la personne doit avoir atteint 18 ans
à la date du jour. Le sélecteur de date empêche également de choisir une date trop récente.

Le mot de passe oublié utilise le modèle standard Firebase : e-mail avec lien sécurisé
de réinitialisation (code d'action intégré au lien, pas un code numérique à six chiffres).

Le ZIP ne contient pas google-services.json, car ce fichier est unique à ton projet Firebase.
