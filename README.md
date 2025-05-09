# ejercicio-3.2
### Los resultados difieren porque:

- Los **hilos de plataforma** se bloquean y retienen un recurso costoso del sistema operativo durante las esperas de I/O (Input/Output), creando un cuello de botella si hay más tareas que hilos. 
- Los **hilos virtuales** se desmontan y liberan el recurso del sistema operativo durante las esperas de I/O, permitiendo una concurrencia masiva de tareas bloqueantes con muy pocos recursos.

### Cuándo elegir cada modelo:

- **Hilos de Plataforma (Pool Fijo):**
  - Principalmente para tareas **intensivas en CPU**, donde es conveniente limitar el número de hilos activos para maximizar la eficiencia.

- **Hilos Virtuales:**
  -  Para tareas limitadas por I/O, es decir tareas que pasan la mayor parte del tiempo esperando respuestas de red, bases de datos, archivos, etc. Son la mejor opción cuando se necesita una muy alta concurrencia. 
