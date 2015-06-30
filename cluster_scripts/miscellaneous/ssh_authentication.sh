cd
ssh node0 mkdir -p .ssh
cat .ssh/id_rsa.pub | ssh node0 'cat >> .ssh/authorized_keys'
