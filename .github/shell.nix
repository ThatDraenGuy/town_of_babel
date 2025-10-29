{
  pkgs ? import <nixpkgs> { },
}:
pkgs.mkShellNoCC {
  preferLocalBuild = true;
  allowSubstitutes = false;

  name = "town-of-babel-ci";

  packages = with pkgs; [
    zizmor
  ];
}
