/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.io.uring.net

import cats.effect.IO
import cats.effect.LiftIO
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._

import com.comcast.ip4s._

import fs2.Stream
import fs2.io.net.Socket
import fs2.io.net.SocketGroup
import fs2.io.net.SocketOption

import fs2.io.uring.Uring
import fs2.io.uring.unsafe.util.createBuffer
import fs2.io.uring.unsafe.util.OP._

import io.netty.incubator.channel.uring.UringSockaddrIn
import io.netty.incubator.channel.uring.UringLinuxSocket
import io.netty.incubator.channel.uring.NativeAccess.SIZEOF_SOCKADDR_IN6

import java.net.Inet6Address

private final class UringSocketGroup[F[_]: LiftIO](implicit F: Async[F], dns: Dns[F])
    extends SocketGroup[F] {
  def client(to: SocketAddress[Host], options: List[SocketOption]): Resource[F, Socket[F]] =
    Resource.eval(Uring.get[F]).flatMap { ring =>
      Resource.eval(to.resolve).flatMap { address =>
        openSocket(ring, address.host.isInstanceOf[Ipv6Address]).flatMap { linuxSocket =>
          Resource.eval {
            createBuffer(SIZEOF_SOCKADDR_IN6).use { buf =>
              val length: Int = UringSockaddrIn.write(
                address.toInetSocketAddress.getAddress.isInstanceOf[Inet6Address],
                buf.memoryAddress(),
                address.toInetSocketAddress
              )
              ring
                .call(IORING_OP_CONNECT, 0, 0, linuxSocket.fd(), buf.memoryAddress(), length, 0)
                .to

            }
          } *> UringSocket(ring, linuxSocket, linuxSocket.fd(), address)
        }
      }
    }

  def server(
      address: Option[Host],
      port: Option[Port],
      options: List[SocketOption]
  ): Stream[F, Socket[F]] = Stream.resource(serverResource(address, port, options)).flatMap(_._2)

  def serverResource(
      address: Option[Host],
      port: Option[Port],
      options: List[SocketOption]
  ): Resource[F, (SocketAddress[IpAddress], Stream[F, Socket[F]])] =
    ???

  private def openSocket(
      ring: Uring,
      ipv6: Boolean
  ): Resource[F, UringLinuxSocket] =
    Resource.make[F, UringLinuxSocket](F.delay(UringLinuxSocket.newSocketStream(ipv6)))(
      linuxSocket => closeSocket(ring, linuxSocket.fd()).to
    )

  private def closeSocket(ring: Uring, fd: Int): IO[Unit] =
    ring.call(op = IORING_OP_CLOSE, fd = fd).void

}

object UringSocketGroup {
  def apply[F[_]: Async: Dns: LiftIO]: SocketGroup[F] = new UringSocketGroup
}
