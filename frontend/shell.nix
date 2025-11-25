{
  pkgs ? import <nixpkgs> { },
}:
pkgs.mkShellNoCC {
  preferLocalBuild = true;
  allowSubstitutes = false;

  name = "town-of-babel-frontend";

  packages = with pkgs; [
    nodejs
  ];
}
