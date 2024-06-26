package com.project.attendance.ServiceImpl;

import com.project.attendance.Config.AppConstants;
import com.project.attendance.Exception.InternalServerException;
import com.project.attendance.Exception.ResourceNotFoundException;
import com.project.attendance.Model.Batch;
import com.project.attendance.Model.Role;
import com.project.attendance.Model.User;
import com.project.attendance.Payload.DTO.UserDTO;
import com.project.attendance.Repository.BatchRepository;
import com.project.attendance.Repository.RoleRepository;
import com.project.attendance.Repository.UserRepository;
import com.project.attendance.Service.UserService;
import com.project.attendance.Utilities.Utility;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository ;

    @Autowired
    BatchRepository batchRepository ;

    @Autowired
    RoleRepository roleRepository ;

    @Autowired
    ModelMapper modelMapper ;

    @Autowired
    PasswordEncoder passwordEncoder ;

    @Autowired
    Utility utility ;

    @Override
    public UserDTO createUser(UserDTO userDTO , String otp) {

        //verify otp
        utility.verifyOtp(userDTO.getEmail() , otp) ;

        User user = modelMapper.map(userDTO , User.class) ;

        user.setJoining_LocalDate(LocalDate.now());
        user.setEnd_LocalDate(user.getJoining_LocalDate().plusMonths(user.getDuration()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        /*roles*/
        Role role = roleRepository.findById(AppConstants.NORMAL_USER).get();
        user.getRoles().add(role);

        User createdUser = userRepository.save(user) ;


        /* Set shift */
        int batchId = 1 ;

        if(createdUser.getShift().equals("Evening")){
            batchId = 2 ;
        }

        this.enrolledToBatch(createdUser.getId() , batchId) ;
        return modelMapper.map(createdUser , UserDTO.class) ;
    }


    @Override
    public List<UserDTO> getAllUser() {

        try{
            List<User> users = userRepository.findNormalUsers() ;

            List<UserDTO> userDTOs = users.stream()
                    .map(user -> modelMapper.map(user , UserDTO.class))
                    .collect(Collectors.toList()) ;

            return userDTOs ;

        }catch (Exception ex) {
            throw new InternalServerException("Internal Server Error");
        }
    }

    @Override
    public User getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User" , "email" + email , 0));

        //System.out.println(user.getRoles().get(0));
        return user ;
    }

    @Override
    public User getUserById(Integer userId) {

        return userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User" , "userId" , userId));
    }

    @Override
    public UserDTO updateUser(UserDTO userDTO, Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User" , "userId" , userId));;

        if (Objects.nonNull(userDTO.getFirstName()) && !userDTO.getFirstName().isEmpty()) {
            user.setFirstName(userDTO.getFirstName());
        }

        if (Objects.nonNull(userDTO.getLastName()) && !userDTO.getLastName().isEmpty()) {
            user.setLastName(userDTO.getLastName());
        }

        if (Objects.nonNull(userDTO.getEnd_LocalDate())) {
            user.setEnd_LocalDate(userDTO.getEnd_LocalDate());
        }


        if (Objects.nonNull(userDTO.getMobile_no()) && !userDTO.getMobile_no().isEmpty()) {
            user.setMobile_no(userDTO.getMobile_no());
        }

        User updatedUser = userRepository.save(user) ;
        return modelMapper.map(updatedUser , UserDTO.class) ;

    }

    @Override
    public void deleteUser(Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User" , "userId" , userId));

        Batch batch = user.getEnrolledBatch();
        batch.getUsers().remove(user) ;

        userRepository.deleteById(userId);
        return ;
    }

    @Override
    public List<UserDTO> getAllUserByShift(String shift) {
        List<User> allUsers = userRepository.findByShift(shift) ;

        List<UserDTO> userDTOs = allUsers.stream()
                .map(user -> modelMapper.map(user , UserDTO.class))
                .collect(Collectors.toList()) ;

        return userDTOs ;
    }

    @Override
    public UserDTO enrolledToBatch(Integer userId ,Integer batchId) {

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(()-> new ResourceNotFoundException("Batch" , "batchId" , batchId));

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User" , "userId" , userId));

        batch.getUsers().add(user) ;
        user.setEnrolledBatch(batch);

        batchRepository.save(batch) ;
        User updatedUser = userRepository.save(user) ;
        return modelMapper.map(updatedUser , UserDTO.class) ;
    }


    @Override
    public User getTrainers(User trainee) {
        return trainee.getTrainer();
    }
}
