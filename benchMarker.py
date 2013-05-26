import paramiko
client = paramiko.SSHClient()
client.load_system_host_keys()
client.set_missing_host_key_policy(paramiko.WarningPolicy)
print '*** Connecting...'
client.connect('mininet-internal', 22, 'mininet','mininet')
(iin,out,err) = client.exec_command('ls -la'); 

## Mandar correr uma topologia do mininet.
## Preciso de saber os ip's das máquinas e dos controladores.

print iin
print out.read() 
print err

client.close()
